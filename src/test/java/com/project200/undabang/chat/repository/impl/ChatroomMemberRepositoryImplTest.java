package com.project200.undabang.chat.repository.impl;

import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.entity.*;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberBlock;
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

    @Nested
    @DisplayName("상대방 상태 조회 (getOpponentStatusByChatroomId)")
    class GetOpponentStatusByChatroomIdTests {

        @Test
        @DisplayName("성공: 상대방이 ACTIVE 상태일 경우 Optional<ACTIVE>를 반환한다")
        void shouldReturnOptionalOfActiveWhenOpponentIsActive() {
            // given
            Member currentUser = createMember("current@example.com", "currentUser");
            Member otherUser = createMember("other@example.com", "otherUser");
            Chatroom chatroom = createChatroom();
            ChatroomMember currentCM = createChatroomMember(chatroom, currentUser, ChatroomMemberStatus.ACTIVE, 0L);
            ChatroomMember otherCM = createChatroomMember(chatroom, otherUser, ChatroomMemberStatus.ACTIVE, 0L);
            persistAndFlush(currentUser, otherUser, chatroom, currentCM, otherCM);

            // when
            Optional<ChatroomMemberStatus> result = chatroomMemberRepository.getOpponentStatusByChatroomId(chatroom.getId(), currentUser);

            // then
            assertThat(result).isPresent().contains(ChatroomMemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("성공: 상대방이 LEFT 상태일 경우 Optional<LEFT>를 반환한다")
        void shouldReturnOptionalOfLeftWhenOpponentHasLeft() {
            // given
            Member currentUser = createMember("current@example.com", "currentUser");
            Member otherUser = createMember("other@example.com", "otherUser");
            Chatroom chatroom = createChatroom();
            ChatroomMember currentCM = createChatroomMember(chatroom, currentUser, ChatroomMemberStatus.ACTIVE, 0L);
            ChatroomMember otherCM = createChatroomMember(chatroom, otherUser, ChatroomMemberStatus.LEFT, 0L); // 상대방은 나간 상태
            persistAndFlush(currentUser, otherUser, chatroom, currentCM, otherCM);

            // when
            Optional<ChatroomMemberStatus> result = chatroomMemberRepository.getOpponentStatusByChatroomId(chatroom.getId(), currentUser);

            // then
            assertThat(result).isPresent().contains(ChatroomMemberStatus.LEFT);
        }
    }

    private void persistAndFlush(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("checkBlockExists 메소드는")
    class CheckBlockExists {

        @Test
        @DisplayName("성공: 현재 사용자가 채팅방의 상대방을 차단했을 경우 true를 반환한다")
        void shouldReturnTrueWhenCurrentUserBlocksOpponent() {
            // given
            Member currentUser = createMember("current@user.com", "currentUser");
            Member otherUser = createMember("other@user.com", "otherUser");
            Chatroom chatroom = createChatroom();
            ChatroomMember currentCM = createChatroomMember(chatroom, currentUser, ChatroomMemberStatus.ACTIVE, 0L);
            ChatroomMember otherCM = createChatroomMember(chatroom, otherUser, ChatroomMemberStatus.ACTIVE, 0L);
            MemberBlock block = createMemberBlock(currentUser, otherUser);
            persistAndFlush(currentUser, otherUser, chatroom, currentCM, otherCM, block);

            // when
            boolean isBlocked = chatroomMemberRepository.checkBlockExists(chatroom, currentUser);

            // then
            assertThat(isBlocked).isTrue();
        }

        @Test
        @DisplayName("성공: 상대방이 현재 사용자를 차단한 경우(역방향)도 true를 반환한다")
        void shouldReturnTrueWhenOpponentBlocksCurrentUser() {
            // given
            Member currentUser = createMember("current@user.com", "currentUser");
            Member otherUser = createMember("other@user.com", "otherUser");
            Chatroom chatroom = createChatroom();
            ChatroomMember currentCM = createChatroomMember(chatroom, currentUser, ChatroomMemberStatus.ACTIVE, 0L);
            ChatroomMember otherCM = createChatroomMember(chatroom, otherUser, ChatroomMemberStatus.ACTIVE, 0L);
            MemberBlock block = createMemberBlock(otherUser, currentUser); // 역방향 차단
            persistAndFlush(currentUser, otherUser, chatroom, currentCM, otherCM, block);

            // when
            boolean isBlocked = chatroomMemberRepository.checkBlockExists(chatroom, currentUser);

            // then
            assertThat(isBlocked).isTrue();
        }

        @Test
        @DisplayName("성공: 아무도 차단하지 않았을 경우 false를 반환한다")
        void shouldReturnFalseWhenNoOneIsBlocked() {
            // given
            Member currentUser = createMember("current@user.com", "currentUser");
            Member otherUser = createMember("other@user.com", "otherUser");
            Chatroom chatroom = createChatroom();
            ChatroomMember currentCM = createChatroomMember(chatroom, currentUser, ChatroomMemberStatus.ACTIVE, 0L);
            ChatroomMember otherCM = createChatroomMember(chatroom, otherUser, ChatroomMemberStatus.ACTIVE, 0L);
            // 차단 기록 없음
            persistAndFlush(currentUser, otherUser, chatroom, currentCM, otherCM);

            // when
            boolean isBlocked = chatroomMemberRepository.checkBlockExists(chatroom, currentUser);

            // then
            assertThat(isBlocked).isFalse();
        }
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

    private ChatroomMember createChatroomMember(Chatroom chatroom, Member member, ChatroomMemberStatus status, Long lastReadChatId) {
        return ChatroomMember.builder()
                .chatroom(chatroom)
                .member(member)
                .chatroomMemberStatus(status)
                .lastReadChatId(lastReadChatId)
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

    private MemberBlock createMemberBlock(Member blocker, Member blocked) {
        return MemberBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();
    }

    @Nested
    class GetChatroomListByMemberIdTests {

        @Test
        void shouldReturnEmptyListWhenNoChatrooms() {
            Member member = createMember("test@example.com", "testUser");
            persistAndFlush(member);

            List<GetMemberChatroomResponse> result = chatroomMemberRepository.getChatroomListByMemberId(member);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnChatroomListWithUnreadCount() {
            // given
            Member currentMember = createMember("current@example.com", "currentUser");
            Member otherMember = createMember("other@example.com", "otherUser");
            Chatroom chatroom = createChatroom();
            ChatroomMember currentCM = createChatroomMember(chatroom, currentMember, ChatroomMemberStatus.ACTIVE, 0L);
            ChatroomMember otherCM = createChatroomMember(chatroom, otherMember, ChatroomMemberStatus.ACTIVE, 0L);
            Chat unreadChat = createChat(chatroom, otherMember, "Unread message", ChatType.USER);
            persistAndFlush(currentMember, otherMember, chatroom, currentCM, otherCM, unreadChat);

            // when
            List<GetMemberChatroomResponse> result = chatroomMemberRepository.getChatroomListByMemberId(currentMember);

            // then
            assertThat(result).hasSize(1);
            GetMemberChatroomResponse response = result.get(0);
            assertThat(response.getOtherMemberNickname()).isEqualTo(otherMember.getMemberNickname());
            assertThat(response.getUnreadCount()).isEqualTo(1L);
            // 추가 검증: 반환된 DTO의 memberId가 실제 상대 멤버의 memberId와 동일한지 확인
            assertThat(response.getOtherMemberId()).isEqualTo(otherMember.getMemberId());
        }
    }
}