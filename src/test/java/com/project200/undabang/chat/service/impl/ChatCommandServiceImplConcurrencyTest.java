package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.chat.entity.ChatroomMemberStatus;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.chat.service.ChatCommandService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("ChatCommandService 데드락 방지 통합 테스트")
class ChatCommandServiceImplConcurrencyTest {

    @Autowired
    private ChatCommandService chatCommandService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ChatroomRepository chatroomRepository;
    @Autowired
    private ChatroomMemberRepository chatroomMemberRepository;
    @Autowired
    private EntityManager em;

    private Member createAndSaveMember(String nickname, String email) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname(nickname)
                .memberEmail(email)
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
        return memberRepository.save(member);
    }

    private Chatroom setupLeftChatroom(Member memberA, Member memberB) {
        Chatroom chatroom = chatroomRepository.save(Chatroom.createChatroom());
        ChatroomMember cmA = ChatroomMember.of(chatroom, memberA);
        cmA.updateMemberStatus(ChatroomMemberStatus.LEFT);
        ChatroomMember cmB = ChatroomMember.of(chatroom, memberB);
        cmB.updateMemberStatus(ChatroomMemberStatus.LEFT);
        chatroomMemberRepository.saveAll(List.of(cmA, cmB));
        return chatroom;
    }

    @Nested
    @DisplayName("채팅방 생성 동시성 환경에서")
    class ConcurrencyTest {

        @Test
        @Transactional
        @DisplayName("두 사용자가 거의 동시에 서로에게 채팅방 생성을 요청해도 데드락이 발생하지 않아야 한다")
        void createChatroom_concurrently_shouldNotCauseDeadlock() throws InterruptedException {
            Member memberA = createAndSaveMember("UserA", "a@test.com");
            Member memberB = createAndSaveMember("UserB", "b@test.com");
            Chatroom chatroom = setupLeftChatroom(memberA, memberB);

            TestTransaction.flagForCommit();
            TestTransaction.end();
            TestTransaction.start();

            // 이제 새로운 트랜잭션이 시작되었으므로, 필요한 엔티티를 다시 조회해야 함
            final Member persistedMemberA = memberRepository.findById(memberA.getMemberId()).get();
            final Member persistedMemberB = memberRepository.findById(memberB.getMemberId()).get();
            final Chatroom persistedChatroom = chatroomRepository.findById(chatroom.getId()).get();

            int threadCount = 20;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger failureCount = new AtomicInteger(0);

            CreateChatroomRequest requestFromA = new CreateChatroomRequest(persistedMemberB.getMemberId());
            CreateChatroomRequest requestFromB = new CreateChatroomRequest(persistedMemberA.getMemberId());

            for (int i = 0; i < threadCount; i++) {
                final boolean isAtoBRequest = i % 2 == 0;

                executorService.submit(() -> {
                    try (MockedStatic<UserContextHolder> mockedUserContext = Mockito.mockStatic(UserContextHolder.class)) {
                        if (isAtoBRequest) {
                            when(UserContextHolder.getUserId()).thenReturn(persistedMemberA.getMemberId());
                            chatCommandService.createChatroom(requestFromA);
                        } else {
                            when(UserContextHolder.getUserId()).thenReturn(persistedMemberB.getMemberId());
                            chatCommandService.createChatroom(requestFromB);
                        }
                    } catch (Exception e) {
                        System.err.println("Exception occurred in thread: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executorService.shutdown();

            assertThat(failureCount.get()).as("데드락 또는 다른 예외가 발생했습니다.").isEqualTo(0);

            ChatroomMember updatedMemberA = chatroomMemberRepository.findByChatroomAndMember(persistedChatroom, persistedMemberA).get();
            ChatroomMember updatedMemberB = chatroomMemberRepository.findByChatroomAndMember(persistedChatroom, persistedMemberB).get();

            assertThat(updatedMemberA.getChatroomMemberStatus()).isEqualTo(ChatroomMemberStatus.ACTIVE);
            assertThat(updatedMemberB.getChatroomMemberStatus()).isEqualTo(ChatroomMemberStatus.ACTIVE);
        }
    }
}