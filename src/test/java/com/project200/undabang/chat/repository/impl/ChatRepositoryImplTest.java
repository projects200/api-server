package com.project200.undabang.chat.repository.impl;

import com.project200.undabang.chat.entity.Chat;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.repository.ChatRepository;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.dto.ChatNotificationContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class ChatRepositoryImplTest {
    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private TestEntityManager em;

    private Member createMember(String nickname) {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname(nickname)
                .memberEmail(nickname + "@test.com")
                .build();
    }

    private Chatroom createChatroom() {
        return Chatroom.createChatroom();
    }

    private Chat createChat(Chatroom chatroom, Member sender, String content) {
        return Chat.of(content, chatroom, sender);
    }

    private <E> E persist(E entity) {
        return em.persistAndFlush(entity);
    }

    @Nested
    @DisplayName("findChatContentForNotification 메소드는")
    class Describe_findChatContentForNotification {

        @Test
        @DisplayName("성공: 채팅 ID로 조회 시, Sender 정보와 채팅방 ID를 포함한 DTO를 반환한다")
        void it_returns_notification_content_successfully() {
            // given
            Member sender = createMember("SenderNick");
            Chatroom chatroom = createChatroom();

            persist(sender);
            persist(chatroom);

            Chat chat = createChat(chatroom, sender, "안녕하세요");
            persist(chat);

            // when
            Optional<ChatNotificationContent> result = chatRepository.findChatContentForNotification(chat.getId());

            // then
            assertThat(result).isPresent();
            ChatNotificationContent content = result.get();

            assertThat(content.memberId()).isEqualTo(sender.getMemberId());
            assertThat(content.memberNickname()).isEqualTo("SenderNick");
            assertThat(content.chatroomId()).isEqualTo(chatroom.getId());
            assertThat(content.chatContent()).isEqualTo("안녕하세요");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅 ID로 조회 시 Optional.empty를 반환한다")
        void it_returns_empty_when_chat_not_found() {
            // given
            Long nonExistentId = 9999L;

            // when
            Optional<ChatNotificationContent> result = chatRepository.findChatContentForNotification(nonExistentId);

            // then
            assertThat(result).isEmpty();
        }
    }
}