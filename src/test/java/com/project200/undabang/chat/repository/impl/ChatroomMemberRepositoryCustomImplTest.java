package com.project200.undabang.chat.repository.impl;

import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.chat.entity.ChatroomMemberStatus;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class ChatroomMemberRepositoryCustomImplTest {

    @Autowired
    private ChatroomMemberRepository chatroomMemberRepository;

    @Autowired
    private TestEntityManager em;

    private void flushAndClear() {
        em.flush();
        em.clear();
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
        return em.persist(Chatroom.createChatroom());
    }

    private ChatroomMember createAndSaveChatroomMember(Chatroom chatroom, Member member, ChatroomMemberStatus status) {
        ChatroomMember chatroomMember = ChatroomMember.builder()
                .chatroom(chatroom)
                .member(member)
                .chatroomMemberStatus(status)
                .build();
        return em.persist(chatroomMember);
    }

    @Nested
    @DisplayName("checkAllMembersActive 메소드는")
    class Describe_checkAllMembersActive {

        @Test
        @DisplayName("채팅방에 활성화된 회원이 2명이면 true를 반환한다")
        void it_returns_true_when_two_active_members() {
            // given
            Chatroom chatroom = createAndSaveChatroom();
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");

            createAndSaveChatroomMember(chatroom, member1, ChatroomMemberStatus.ACTIVE);
            createAndSaveChatroomMember(chatroom, member2, ChatroomMemberStatus.ACTIVE);

            flushAndClear();

            // when
            boolean result = chatroomMemberRepository.checkAllMembersActive(chatroom.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("채팅방에 비활성화된 회원이 있으면 false를 반환한다")
        void it_returns_false_when_has_inactive_member() {
            // given
            Chatroom chatroom = createAndSaveChatroom();
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");

            createAndSaveChatroomMember(chatroom, member1, ChatroomMemberStatus.ACTIVE);
            createAndSaveChatroomMember(chatroom, member2, ChatroomMemberStatus.LEFT);

            flushAndClear();

            // when
            boolean result = chatroomMemberRepository.checkAllMembersActive(chatroom.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("활성화된 회원이 2명보다 많으면 false를 반환한다")
        void it_returns_false_when_more_than_two_active_members() {
            // given
            Chatroom chatroom = createAndSaveChatroom();
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");
            Member member3 = createAndSaveMember("user3");

            createAndSaveChatroomMember(chatroom, member1, ChatroomMemberStatus.ACTIVE);
            createAndSaveChatroomMember(chatroom, member2, ChatroomMemberStatus.ACTIVE);
            createAndSaveChatroomMember(chatroom, member3, ChatroomMemberStatus.ACTIVE);

            flushAndClear();

            // when
            boolean result = chatroomMemberRepository.checkAllMembersActive(chatroom.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("활성화된 회원이 2명보다 적으면 false를 반환한다")
        void it_returns_false_when_less_than_two_active_members() {
            // given
            Chatroom chatroom = createAndSaveChatroom();
            Member member1 = createAndSaveMember("user1");

            createAndSaveChatroomMember(chatroom, member1, ChatroomMemberStatus.ACTIVE);

            flushAndClear();

            // when
            boolean result = chatroomMemberRepository.checkAllMembersActive(chatroom.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("채팅방이 존재하지 않으면 false를 반환한다")
        void it_returns_false_when_chatroom_not_exists() {
            // given
            Long nonExistentChatroomId = 9999L;

            // when
            boolean result = chatroomMemberRepository.checkAllMembersActive(nonExistentChatroomId);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("채팅방은 존재하지만 회원이 없는 경우 false를 반환한다")
        void it_returns_false_when_chatroom_exists_but_no_members() {
            // given
            Chatroom emptyChatroom = createAndSaveChatroom();

            flushAndClear();

            // when
            boolean result = chatroomMemberRepository.checkAllMembersActive(emptyChatroom.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("채팅방에 회원이 있지만 모두 비활성 상태인 경우 false를 반환한다")
        void it_returns_false_when_all_members_inactive() {
            // given
            Chatroom chatroom = createAndSaveChatroom();
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");

            createAndSaveChatroomMember(chatroom, member1, ChatroomMemberStatus.LEFT);
            createAndSaveChatroomMember(chatroom, member2, ChatroomMemberStatus.LEFT);

            flushAndClear();

            // when
            boolean result = chatroomMemberRepository.checkAllMembersActive(chatroom.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("채팅방에 활성 회원이 1명, 비활성 회원이 1명 있는 경우 false를 반환한다")
        void it_returns_false_when_one_active_one_inactive() {
            // given
            Chatroom chatroom = createAndSaveChatroom();
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");

            createAndSaveChatroomMember(chatroom, member1, ChatroomMemberStatus.ACTIVE);
            createAndSaveChatroomMember(chatroom, member2, ChatroomMemberStatus.LEFT);

            flushAndClear();

            // when
            boolean result = chatroomMemberRepository.checkAllMembersActive(chatroom.getId());

            // then
            assertThat(result).isFalse();
        }
    }
}