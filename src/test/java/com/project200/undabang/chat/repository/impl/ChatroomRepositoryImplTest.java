package com.project200.undabang.chat.repository.impl;

import com.project200.undabang.chat.dto.response.ChatMessageDto;
import com.project200.undabang.chat.entity.Chat;
import com.project200.undabang.chat.entity.ChatType;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class ChatroomRepositoryImplTest {

    @Autowired
    private ChatroomRepository chatroomRepository;

    @Autowired
    private TestEntityManager em;

    @Nested
    @DisplayName("findChatroomBetweenMembers 메소드는")
    class Describe_findChatroomBetweenMembers {

        @Test
        @DisplayName("두 회원 간의 채팅방이 존재하면 채팅방을 반환한다")
        void it_returns_chatroom_when_exists_between_members() {
            // given
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");

            Chatroom chatroom = createAndSaveChatroom();

            createAndSaveChatroomMember(chatroom, member1);
            createAndSaveChatroomMember(chatroom, member2);

            flushAndClear();

            // when
            Optional<Chatroom> result = chatroomRepository.findChatroomBetweenMembers(member1, member2);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(chatroom.getId());
        }

        @Test
        @DisplayName("두 회원 간의 채팅방이 존재하지 않으면 빈 Optional을 반환한다")
        void it_returns_empty_when_chatroom_not_exists() {
            // given
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");

            flushAndClear();

            // when
            Optional<Chatroom> result = chatroomRepository.findChatroomBetweenMembers(member1, member2);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("두 회원이 모두 포함된 채팅방을 반환한다")
        void it_returns_chatroom_when_both_members_included() {
            // given
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");
            Member member3 = createAndSaveMember("user3");

            Chatroom chatroom = createAndSaveChatroom();

            createAndSaveChatroomMember(chatroom, member1);
            createAndSaveChatroomMember(chatroom, member2);
            createAndSaveChatroomMember(chatroom, member3);

            flushAndClear();

            // when
            Optional<Chatroom> result = chatroomRepository.findChatroomBetweenMembers(member1, member2);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(chatroom.getId());
        }


        @Test
        @DisplayName("회원 한 명만 있는 채팅방은 반환하지 않는다")
        void it_returns_empty_for_single_member_chatroom() {
            // given
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");

            Chatroom chatroom = createAndSaveChatroom();

            createAndSaveChatroomMember(chatroom, member1);

            flushAndClear();

            // when
            Optional<Chatroom> result = chatroomRepository.findChatroomBetweenMembers(member1, member2);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("두 회원 모두 포함하지 않는 채팅방은 반환하지 않는다")
        void it_returns_empty_when_chatroom_does_not_include_members() {
            // given
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");
            Member otherMember1 = createAndSaveMember("other1");
            Member otherMember2 = createAndSaveMember("other2");

            Chatroom chatroom = createAndSaveChatroom();

            createAndSaveChatroomMember(chatroom, otherMember1);
            createAndSaveChatroomMember(chatroom, otherMember2);

            flushAndClear();

            // when
            Optional<Chatroom> result = chatroomRepository.findChatroomBetweenMembers(member1, member2);

            // then
            assertThat(result).isEmpty();
        }
    }

    private Chat createAndSaveChat(Chatroom chatroom, Member sender, String content, LocalDateTime createdAt) {
        Chat chat = Chat.builder()
                .chatroom(chatroom)
                .sender(sender)
                .chatContent(content)
                .chatType(ChatType.USER)
                .chatCreatedAt(createdAt)
                .build();
        return em.persist(chat);
    }

    private Member createAndSaveMember(String nickname) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@example.com")
                .memberNickname(nickname)
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(1990, 1, 1))
                .build();
        return em.persist(member);
    }

    private Chatroom createAndSaveChatroom() {
        Chatroom chatroom = Chatroom.createChatroom();
        return em.persist(chatroom);
    }

    private ChatroomMember createAndSaveChatroomMember(Chatroom chatroom, Member member) {
        ChatroomMember chatroomMember = ChatroomMember.of(chatroom, member);
        return em.persist(chatroomMember);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("getMemberChat 메소드는")
    class Describe_getMemberChat {

        @Test
        @DisplayName("첫 페이지의 메시지 목록을 시간순으로 올바르게 반환하고, 다음 페이지가 있음을 알린다")
        void it_returns_first_page_and_indicates_has_next() {
            // given: 총 5개의 메시지를 생성하고, 페이지 사이즈는 3으로 설정
            Member currentUser = createAndSaveMember("currentUser");
            Member otherUser = createAndSaveMember("otherUser");
            Chatroom chatroom = createAndSaveChatroom();
            createAndSaveChatroomMember(chatroom, currentUser);
            createAndSaveChatroomMember(chatroom, otherUser);

            // 5개의 메시지 생성 (ID 1~5)
            List<Chat> chats = IntStream.rangeClosed(1, 5)
                    .mapToObj(i -> createAndSaveChat(chatroom, i % 2 == 0 ? currentUser : otherUser, "메시지 " + i, LocalDateTime.now().plusMinutes(i)))
                    .collect(Collectors.toList());

            flushAndClear();

            Pageable pageable = PageRequest.of(0, 3);
            Long prevChatId = null; // 첫 페이지 조회이므로 커서는 null

            // when
            Slice<ChatMessageDto> result = chatroomRepository.getMemberChat(chatroom.getId(), prevChatId, pageable, currentUser);

            // then
            assertThat(result).isNotNull();
            assertThat(result.hasNext()).isTrue(); // 5개 중 3개만 가져왔으므로 다음 페이지가 있음
            assertThat(result.getContent()).hasSize(3);

            // DB에서는 최신순(5,4,3)으로 가져오지만, 서비스에서 reverse 하므로 최종 결과는 시간순(3,4,5)이어야 함
            assertThat(result.getContent())
                    .extracting(ChatMessageDto::getChatId)
                    .containsExactly(chats.get(2).getId(), chats.get(3).getId(), chats.get(4).getId());

            // isMine 필드 검증
            ChatMessageDto myMessage = result.getContent().get(1); // 4번 메시지 (currentUser가 보냄)
            ChatMessageDto otherMessage = result.getContent().get(0); // 3번 메시지 (otherUser가 보냄)
            assertThat(myMessage.isMine()).isTrue();
            assertThat(otherMessage.isMine()).isFalse();
        }

        @Test
        @DisplayName("커서(prevChatId)를 사용하여 다음 페이지의 메시지 목록을 올바르게 반환한다")
        void it_returns_next_page_using_cursor() {
            // given: 총 5개의 메시지를 생성하고, 두 번째 페이지를 조회
            Member currentUser = createAndSaveMember("currentUser");
            Member otherUser = createAndSaveMember("otherUser");
            Chatroom chatroom = createAndSaveChatroom();
            createAndSaveChatroomMember(chatroom, currentUser);
            createAndSaveChatroomMember(chatroom, otherUser);

            List<Chat> chats = IntStream.rangeClosed(1, 5)
                    .mapToObj(i -> createAndSaveChat(chatroom, currentUser, "메시지 " + i, LocalDateTime.now().plusMinutes(i)))
                    .collect(Collectors.toList());

            flushAndClear();

            Pageable pageable = PageRequest.of(0, 3);
            // 첫 페이지는 (id: 3,4,5)를 반환. 따라서 다음 페이지를 위한 커서는 3이 됨.
            Long prevChatId = chats.get(2).getId();

            // when
            Slice<ChatMessageDto> result = chatroomRepository.getMemberChat(chatroom.getId(), prevChatId, pageable, currentUser);

            // then
            assertThat(result).isNotNull();
            assertThat(result.hasNext()).isFalse(); // 남은 2개가 마지막이므로 다음 페이지 없음
            assertThat(result.getContent()).hasSize(2);

            // 커서(3) 이전의 메시지 (1,2)가 시간순으로 반환되어야 함
            assertThat(result.getContent())
                    .extracting(ChatMessageDto::getChatId)
                    .containsExactly(chats.get(0).getId(), chats.get(1).getId());
        }

        @Test
        @DisplayName("마지막 페이지일 경우, hasNext를 false로 반환한다")
        void it_returns_last_page_and_indicates_no_next() {
            // given: 페이지 사이즈와 동일한 3개의 메시지만 생성
            Member currentUser = createAndSaveMember("currentUser");
            Member otherUser = createAndSaveMember("otherUser");
            Chatroom chatroom = createAndSaveChatroom();
            createAndSaveChatroomMember(chatroom, currentUser);
            createAndSaveChatroomMember(chatroom, otherUser);

            List<Chat> chats = IntStream.rangeClosed(1, 3)
                    .mapToObj(i -> createAndSaveChat(chatroom, currentUser, "메시지 " + i, LocalDateTime.now().plusMinutes(i)))
                    .collect(Collectors.toList());

            flushAndClear();

            Pageable pageable = PageRequest.of(0, 3);
            Long prevChatId = null;

            // when
            Slice<ChatMessageDto> result = chatroomRepository.getMemberChat(chatroom.getId(), prevChatId, pageable, currentUser);

            // then
            assertThat(result).isNotNull();
            assertThat(result.hasNext()).isFalse(); // 정확히 3개만 있으므로 다음 페이지 없음
            assertThat(result.getContent()).hasSize(3);
        }

        @Test
        @DisplayName("채팅 내용이 없는 채팅방의 경우, 빈 목록과 hasNext false를 반환한다")
        void it_returns_empty_list_for_empty_chatroom() {
            // given
            Member currentUser = createAndSaveMember("currentUser");
            Member otherUser = createAndSaveMember("otherUser");
            Chatroom chatroom = createAndSaveChatroom();
            createAndSaveChatroomMember(chatroom, currentUser);
            createAndSaveChatroomMember(chatroom, otherUser);

            flushAndClear();

            Pageable pageable = PageRequest.of(0, 30);
            Long prevChatId = null;

            // when
            Slice<ChatMessageDto> result = chatroomRepository.getMemberChat(chatroom.getId(), prevChatId, pageable, currentUser);

            // then
            assertThat(result).isNotNull();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.getContent()).isEmpty();
        }
    }
}