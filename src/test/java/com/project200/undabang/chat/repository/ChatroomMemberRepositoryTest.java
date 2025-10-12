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
        ChatroomMember chatroomMember = ChatroomMember.of(chatroom, member);
        return em.persist(chatroomMember);
    }

    @Nested
    @DisplayName("findByChatroom_IdAndMember_MemberId 메소드는")
    class Describe_findByChatroomIdAndMemberId {

        @Test
        @DisplayName("채팅방 ID와 회원 ID로 ChatroomMember를 찾아 반환한다")
        void it_returns_chatroom_member_when_found() {
            // given
            Member member = createAndSaveMember("user1");
            Chatroom chatroom = createAndSaveChatroom();
            ChatroomMember expectedChatroomMember = createAndSaveChatroomMember(chatroom, member);

            flushAndClear();

            // when
            Optional<ChatroomMember> result = chatroomMemberRepository.findByChatroom_IdAndMember_MemberId(chatroom.getId(), member.getMemberId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getChatroomMemberId()).isEqualTo(expectedChatroomMember.getChatroomMemberId());
            assertThat(result.get().getMember().getMemberId()).isEqualTo(member.getMemberId());
        }

        @Test
        @DisplayName("회원은 존재하지만 다른 채팅방에 속해있을 경우 빈 Optional을 반환한다")
        void it_returns_empty_when_member_in_different_chatroom() {
            // given
            Member member = createAndSaveMember("user1");
            Chatroom chatroomToSearch = createAndSaveChatroom(); // 검색할 채팅방
            Chatroom otherChatroom = createAndSaveChatroom();    // 멤버가 실제 속한 채팅방
            createAndSaveChatroomMember(otherChatroom, member);

            flushAndClear();

            // when
            // 멤버가 속하지 않은 chatroomToSearch에서 검색
            Optional<ChatroomMember> result = chatroomMemberRepository.findByChatroom_IdAndMember_MemberId(chatroomToSearch.getId(), member.getMemberId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("채팅방은 존재하지만 다른 회원이 속해있을 경우 빈 Optional을 반환한다")
        void it_returns_empty_when_chatroom_has_different_member() {
            // given
            Member memberToSearch = createAndSaveMember("userToSearch");
            Member otherMember = createAndSaveMember("otherUser");
            Chatroom chatroom = createAndSaveChatroom();
            createAndSaveChatroomMember(chatroom, otherMember);

            flushAndClear();

            // when
            // 다른 회원이 속한 채팅방에서 memberToSearch를 검색
            Optional<ChatroomMember> result = chatroomMemberRepository.findByChatroom_IdAndMember_MemberId(chatroom.getId(), memberToSearch.getMemberId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("일치하는 ChatroomMember가 없으면 빈 Optional을 반환한다")
        void it_returns_empty_when_not_exists() {
            // given
            Long nonExistentChatroomId = 999L;
            UUID nonExistentMemberId = UUID.randomUUID();

            // when
            Optional<ChatroomMember> result = chatroomMemberRepository.findByChatroom_IdAndMember_MemberId(nonExistentChatroomId, nonExistentMemberId);

            // then
            assertThat(result).isEmpty();
        }
    }
}