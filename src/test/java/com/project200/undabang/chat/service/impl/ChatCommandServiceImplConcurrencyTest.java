package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.chat.entity.ChatroomMemberStatus;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.chat.service.ChatCommandService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.service.PolicyService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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

    @Autowired
    private ExerciseLocationRepository exerciseLocationRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory();
    @MockitoBean
    private PolicyService policyService;

    private Member createAndSaveMember(String nickname, String email) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname(nickname)
                .memberEmail(email)
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
        return memberRepository.save(member);
    }

    private ExerciseLocation createAndSaveExerciseLocation(Member member, double lat, double lon) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        ExerciseLocation location = ExerciseLocation.builder()
                .member(member)
                .exerciseLocationName("테스트 운동장소")
                .exerciseLocationAddress("서울시 테스트구 테스트동")
                .exerciseLocationPoint(point)
                .build();
        return exerciseLocationRepository.save(location);
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

    private CreateChatroomRequest createChatroomRequest(Member targetMember, ExerciseLocation targetLocation) {
        return new CreateChatroomRequest(
                targetMember.getMemberId(),
                targetLocation.getExerciseLocationId(),
                targetLocation.getExerciseLocationPoint().getY(), // Latitude
                targetLocation.getExerciseLocationPoint().getX()  // Longitude
        );
    }

    @Nested
    @DisplayName("채팅방 생성 동시성 환경에서")
    class ConcurrencyTest {

        @Test
        @Transactional
        @DisplayName("두 사용자가 거의 동시에 서로에게 채팅방 생성을 요청해도 데드락이 발생하지 않아야 한다")
        void createChatroom_concurrently_shouldNotCauseDeadlock() throws InterruptedException {
            // 정책 서비스가 무조건 통과되는 거리(50km)를 반환하도록 설정
            given(policyService.getPolicyValueAsDouble(any())).willReturn(50000.0);

            Member memberA = createAndSaveMember("UserA", "a@test.com");
            Member memberB = createAndSaveMember("UserB", "b@test.com");

            ExerciseLocation locationA = createAndSaveExerciseLocation(memberA, 37.5559, 126.9723);
            ExerciseLocation locationB = createAndSaveExerciseLocation(memberB, 37.5298, 126.9647);

            Chatroom chatroom = setupLeftChatroom(memberA, memberB);

            TestTransaction.flagForCommit();
            TestTransaction.end();
            TestTransaction.start();

            // 영속성 컨텍스트 재로딩
            final Member persistedMemberA = memberRepository.findById(memberA.getMemberId()).get();
            final Member persistedMemberB = memberRepository.findById(memberB.getMemberId()).get();
            final ExerciseLocation persistedLocationA = exerciseLocationRepository.findById(locationA.getExerciseLocationId()).get();
            final ExerciseLocation persistedLocationB = exerciseLocationRepository.findById(locationB.getExerciseLocationId()).get();
            final Chatroom persistedChatroom = chatroomRepository.findById(chatroom.getId()).get();

            int threadCount = 20;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger failureCount = new AtomicInteger(0);

            CreateChatroomRequest requestFromA = createChatroomRequest(persistedMemberB, persistedLocationB); // A -> B
            CreateChatroomRequest requestFromB = createChatroomRequest(persistedMemberA, persistedLocationA); // B -> A

            // 동시성 테스트 실행
            for (int i = 0; i < threadCount; i++) {
                final boolean isAtoBRequest = i % 2 == 0;

                executorService.submit(() -> {
                    try (MockedStatic<UserContextHolder> mockedUserContext = Mockito.mockStatic(UserContextHolder.class)) {
                        if (isAtoBRequest) {
                            mockedUserContext.when(UserContextHolder::getUserId).thenReturn(persistedMemberA.getMemberId());
                            chatCommandService.createChatroom(requestFromA);
                        } else {
                            mockedUserContext.when(UserContextHolder::getUserId).thenReturn(persistedMemberB.getMemberId());
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

            assertThat(failureCount.get()).as("데드락 또는 예외가 발생했습니다.").isEqualTo(0);
            em.clear();

            ChatroomMember updatedMemberA = chatroomMemberRepository.findByChatroomAndMember(persistedChatroom, persistedMemberA).get();
            ChatroomMember updatedMemberB = chatroomMemberRepository.findByChatroomAndMember(persistedChatroom, persistedMemberB).get();

            assertThat(updatedMemberA.getChatroomMemberStatus()).isEqualTo(ChatroomMemberStatus.ACTIVE);
            assertThat(updatedMemberB.getChatroomMemberStatus()).isEqualTo(ChatroomMemberStatus.ACTIVE);
        }
    }
}