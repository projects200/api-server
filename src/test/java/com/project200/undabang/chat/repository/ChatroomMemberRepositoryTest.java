package com.project200.undabang.chat.repository;


import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.chat.entity.ChatroomMemberStatus;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class ChatroomMemberRepositoryTest {

    @Autowired
    private ChatroomMemberRepository chatroomMemberRepository;

    @Autowired
    private TestEntityManager em;

    @Nested
    @DisplayName("findByChatroomAndMember 메소드는")
    class Describe_findByChatroomAndMember {

        @Test
        @DisplayName("채팅방과 회원으로 ChatroomMember를 찾아 반환한다")
        void it_returns_chatroom_member_when_exists() {
            // given
            Member member = createAndSaveMember("user1");
            Chatroom chatroom = createAndSaveChatroom();
            ChatroomMember chatroomMember = createAndSaveChatroomMember(chatroom, member);

            flushAndClear();

            // when
            Optional<ChatroomMember> result = chatroomMemberRepository.findByChatroomAndMember(chatroom, member);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getChatroomMemberId()).isEqualTo(chatroomMember.getChatroomMemberId());
            assertThat(result.get().getMember().getMemberId()).isEqualTo(member.getMemberId());
            assertThat(result.get().getChatroom().getId()).isEqualTo(chatroom.getId());
        }

        @Test
        @DisplayName("채팅방과 회원에 해당하는 ChatroomMember가 없으면 빈 Optional을 반환한다")
        void it_returns_empty_when_not_exists() {
            // given
            Member member = createAndSaveMember("user1");
            Chatroom chatroom = createAndSaveChatroom();

            // chatroomMember를 생성하지 않음
            flushAndClear();

            // when
            Optional<ChatroomMember> result = chatroomMemberRepository.findByChatroomAndMember(chatroom, member);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByChatroom_IdAndMember 메소드는")
    class Describe_findByChatroom_IdAndMember {

        @Test
        @DisplayName("채팅방 ID와 회원 엔티티로 ChatroomMember를 찾아 반환한다")
        void it_returns_chatroom_member_when_exists() {
            // given
            Member member = createAndSaveMember("user1");
            Chatroom chatroom = createAndSaveChatroom();
            ChatroomMember expected = createAndSaveChatroomMember(chatroom, member);

            flushAndClear();

            // when
            Optional<ChatroomMember> result = chatroomMemberRepository.findByChatroom_IdAndMember(chatroom.getId(), member);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getChatroomMemberId()).isEqualTo(expected.getChatroomMemberId());
            assertThat(result.get().getMember().getMemberId()).isEqualTo(member.getMemberId());
            assertThat(result.get().getChatroom().getId()).isEqualTo(chatroom.getId());
        }

        @Test
        @DisplayName("다른 채팅방의 멤버일 경우 결과를 반환하지 않는다")
        void it_returns_empty_when_member_in_another_chatroom() {
            // given
            Member member = createAndSaveMember("user1");
            Chatroom chatroom1 = createAndSaveChatroom();
            Chatroom chatroom2 = createAndSaveChatroom(); // 다른 채팅방
            createAndSaveChatroomMember(chatroom1, member); // 멤버는 chatroom1에만 속함

            flushAndClear();

            // when
            // chatroom2에서 해당 멤버를 찾으려고 시도
            Optional<ChatroomMember> result = chatroomMemberRepository.findByChatroom_IdAndMember(chatroom2.getId(), member);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("채팅방과 회원에 해당하는 ChatroomMember가 없으면 빈 Optional을 반환한다")
        void it_returns_empty_when_not_exists() {
            // given
            Member member = createAndSaveMember("user1");
            Chatroom chatroom = createAndSaveChatroom();
            // ChatroomMember를 생성하지 않음

            flushAndClear();

            // when
            Optional<ChatroomMember> result = chatroomMemberRepository.findByChatroom_IdAndMember(chatroom.getId(), member);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByChatroomAndChatroomMemberStatus 메소드는")
    class Describe_countByChatroomAndChatroomMemberStatus {

        @Test
        @DisplayName("채팅방과 상태에 해당하는 ChatroomMember 수를 반환한다")
        void it_returns_count_of_chatroom_members_with_status() {
            // given
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");
            Chatroom chatroom = createAndSaveChatroom();
            createAndSaveChatroomMember(chatroom, member1);
            createAndSaveChatroomMember(chatroom, member2);

            flushAndClear();

            // when
            long count = chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE);

            // then
            assertThat(count).isEqualTo(2L);
        }

        @Test
        @DisplayName("채팅방과 상태에 해당하는 ChatroomMember가 없으면 0을 반환한다")
        void it_returns_zero_when_no_members_with_status() {
            // given
            Member member = createAndSaveMember("user1");
            Chatroom chatroom = createAndSaveChatroom();
            ChatroomMember chatroomMember = createAndSaveChatroomMember(chatroom, member);
            chatroomMember.updateMemberStatus(ChatroomMemberStatus.LEFT); // 상태 변경
            em.persist(chatroomMember);

            flushAndClear();

            // when
            long count = chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE);

            // then
            assertThat(count).isEqualTo(0L);
        }

        @Test
        @DisplayName("다른 채팅방의 멤버는 카운트하지 않는다")
        void it_does_not_count_members_from_other_chatrooms() {
            // given
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");
            Chatroom chatroom1 = createAndSaveChatroom();
            Chatroom chatroom2 = createAndSaveChatroom();

            // chatroom1에 ACTIVE 멤버 1명
            createAndSaveChatroomMember(chatroom1, member1, ChatroomMemberStatus.ACTIVE);
            // chatroom2에 ACTIVE 멤버 1명
            createAndSaveChatroomMember(chatroom2, member2, ChatroomMemberStatus.ACTIVE);

            flushAndClear();

            // when
            // chatroom1의 멤버만 카운트
            long count = chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom1, ChatroomMemberStatus.ACTIVE);

            // then
            assertThat(count).isEqualTo(1L);
        }
    }

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

    private ChatroomMember createAndSaveChatroomMember(Chatroom chatroom, Member member) {
        return createAndSaveChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);
    }

    private ChatroomMember createAndSaveChatroomMember(Chatroom chatroom, Member member, ChatroomMemberStatus status) {
        ChatroomMember chatroomMember = ChatroomMember.of(chatroom, member);
        if (status != ChatroomMemberStatus.ACTIVE) {
            chatroomMember.updateMemberStatus(status);
        }
        return em.persist(chatroomMember);
    }
}