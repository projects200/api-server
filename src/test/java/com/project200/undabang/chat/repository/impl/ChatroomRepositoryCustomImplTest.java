package com.project200.undabang.chat.repository.impl;

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

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class ChatroomRepositoryCustomImplTest {

    @Autowired
    private ChatroomRepository chatroomRepository;

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
        Chatroom chatroom = Chatroom.createChatroom();
        return em.persist(chatroom);
    }

    private ChatroomMember createAndSaveChatroomMember(Chatroom chatroom, Member member) {
        ChatroomMember chatroomMember = ChatroomMember.of(chatroom, member);
        return em.persist(chatroomMember);
    }

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
}