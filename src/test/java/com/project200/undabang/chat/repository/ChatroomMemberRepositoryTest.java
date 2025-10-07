package com.project200.undabang.chat.repository;


import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
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
}