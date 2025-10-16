package com.project200.undabang.chat.repository.impl;

import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.entity.*;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class ChatroomMemberRepositoryImplTest {

    @Autowired
    private ChatroomMemberRepository chatroomMemberRepository;

    @Autowired
    private EntityManager em;

    private void persistAndFlush(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }

    private Member createMember(String email, String nickname) {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(email)
                .memberNickname(nickname)
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(1990, 1, 1))
                .build();
    }

    private Chatroom createChatroom() {
        return Chatroom.builder().build();
    }

    private ChatroomMember createChatroomMember(Chatroom chatroom, Member member, Long lastReadChatId) {
        return ChatroomMember.builder()
                .chatroom(chatroom)
                .member(member)
                .lastReadChatId(lastReadChatId)
                .chatroomMemberStatus(ChatroomMemberStatus.ACTIVE)
                .build();
    }

    // [추가] 상태(Status)를 지정할 수 있는 새로운 헬퍼 메서드 (오버로딩)
    private ChatroomMember createChatroomMember(Chatroom chatroom, Member member, ChatroomMemberStatus status) {
        return ChatroomMember.builder()
                .chatroom(chatroom)
                .member(member)
                .chatroomMemberStatus(status)
                .build();
    }

    private Chat createChat(Chatroom chatroom, Member sender, String content, ChatType chatType) {
        return Chat.builder()
                .chatroom(chatroom)
                .sender(sender)
                .chatContent(content)
                .chatType(chatType)
                .build();
    }

    @Nested
    class GetChatroomListByMemberIdTests {

        @Test
        void shouldReturnEmptyListWhenNoChatrooms() {
            // Given
            Member member = createMember("test@example.com", "testUser");
            persistAndFlush(member);

            // When
            List<GetMemberChatroomResponse> result = chatroomMemberRepository.getChatroomListByMemberId(member);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnChatroomListWithUnreadCount() {
            // Given
            Member currentMember = createMember("current@example.com", "currentUser");
            Member otherMember = createMember("other@example.com", "otherUser");
            Chatroom chatroom = createChatroom();
            ChatroomMember currentCM = createChatroomMember(chatroom, currentMember, 0L);
            ChatroomMember otherCM = createChatroomMember(chatroom, otherMember, 0L);
            Chat unreadChat = createChat(chatroom, otherMember, "Unread message", ChatType.USER);

            persistAndFlush(currentMember, otherMember, chatroom, currentCM, otherCM, unreadChat);

            // When
            List<GetMemberChatroomResponse> result = chatroomMemberRepository.getChatroomListByMemberId(currentMember);

            // Then
            assertThat(result).hasSize(1);
            GetMemberChatroomResponse response = result.get(0);
            assertThat(response.getChatRoomId()).isEqualTo(chatroom.getId());
            assertThat(response.getOtherMemberNickname()).isEqualTo(otherMember.getMemberNickname());
            assertThat(response.getUnreadCount()).isEqualTo(1L);
        }

        @Test
        void shouldIgnoreSystemMessagesInUnreadCount() {
            // Given
            Member currentMember = createMember("current@example.com", "currentUser");
            Member otherMember = createMember("other@example.com", "otherUser");
            Chatroom chatroom = createChatroom();
            ChatroomMember currentCM = createChatroomMember(chatroom, currentMember, 0L);
            ChatroomMember otherCM = createChatroomMember(chatroom, otherMember, 0L);
            Chat systemChat = createChat(chatroom, null, "System message", ChatType.SYSTEM);
            Chat userChat = createChat(chatroom, otherMember, "User message", ChatType.USER);

            persistAndFlush(currentMember, otherMember, chatroom, currentCM, otherCM, systemChat, userChat);

            // When
            List<GetMemberChatroomResponse> result = chatroomMemberRepository.getChatroomListByMemberId(currentMember);

            // Then
            assertThat(result).hasSize(1);
            GetMemberChatroomResponse response = result.get(0);
            assertThat(response.getUnreadCount()).isEqualTo(1L); // 시스템 메시지는 무시
        }
    }

    @Nested
    @DisplayName("상대방 상태 조회 (getOpponentStatusByChatroomId)")
    class GetOpponentStatusByChatroomIdTests {

        @Test
        @DisplayName("성공: 상대방이 ACTIVE 상태일 경우 Optional<ACTIVE>를 반환한다")
        void shouldReturnOptionalOfActiveWhenOpponentIsActive() {
            // Given: 각 테스트에 필요한 모든 엔티티를 헬퍼 메서드를 통해 생성
            Member currentUser = createMember("current@example.com", "currentUser");
            Member otherUser = createMember("other@example.com", "otherUser");
            Chatroom chatroom = createChatroom();
            ChatroomMember currentCM = createChatroomMember(chatroom, currentUser, ChatroomMemberStatus.ACTIVE);
            ChatroomMember otherCM = createChatroomMember(chatroom, otherUser, ChatroomMemberStatus.ACTIVE);
            persistAndFlush(currentUser, otherUser, chatroom, currentCM, otherCM);

            // When
            Optional<ChatroomMemberStatus> result = chatroomMemberRepository.getOpponentStatusByChatroomId(chatroom.getId(), currentUser);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(ChatroomMemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("성공: 상대방이 LEFT 상태일 경우 Optional<LEFT>를 반환한다")
        void shouldReturnOptionalOfLeftWhenOpponentHasLeft() {
            // Given
            Member currentUser = createMember("current@example.com", "currentUser");
            Member otherUser = createMember("other@example.com", "otherUser");
            Chatroom chatroom = createChatroom();
            ChatroomMember currentCM = createChatroomMember(chatroom, currentUser, ChatroomMemberStatus.ACTIVE);
            ChatroomMember otherCM = createChatroomMember(chatroom, otherUser, ChatroomMemberStatus.LEFT); // 상대방은 나간 상태
            persistAndFlush(currentUser, otherUser, chatroom, currentCM, otherCM);

            // When
            Optional<ChatroomMemberStatus> result = chatroomMemberRepository.getOpponentStatusByChatroomId(chatroom.getId(), currentUser);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(ChatroomMemberStatus.LEFT);
        }

        @Test
        @DisplayName("성공: 1:1 채팅이 아니거나 상대방이 없을 경우 빈 Optional을 반환한다")
        void shouldReturnEmptyOptionalWhenNoOpponent() {
            // Given: 채팅방에 현재 사용자만 있는 경우
            Member currentUser = createMember("current@example.com", "currentUser");
            Chatroom chatroom = createChatroom();
            ChatroomMember currentCM = createChatroomMember(chatroom, currentUser, ChatroomMemberStatus.ACTIVE);
            persistAndFlush(currentUser, chatroom, currentCM);

            // When
            Optional<ChatroomMemberStatus> result = chatroomMemberRepository.getOpponentStatusByChatroomId(chatroom.getId(), currentUser);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
