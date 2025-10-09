package com.project200.undabang.chat.repository.impl;

import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.entity.*;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
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
        return Chatroom.builder()
                .build();
    }

    private ChatroomMember createChatroomMember(Chatroom chatroom, Member member, Long lastReadChatId) {
        return ChatroomMember.builder()
                .chatroom(chatroom)
                .member(member)
                .lastReadChatId(lastReadChatId)
                .chatroomMemberStatus(ChatroomMemberStatus.ACTIVE)
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
            ChatroomMember otherCM = createChatroomMember(chatroom, otherMember, null);
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
            ChatroomMember otherCM = createChatroomMember(chatroom, otherMember, null);
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
}
