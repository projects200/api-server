package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.chat.dto.event.ChatMessageCreatedEvent;
import com.project200.undabang.chat.repository.ChatRepository;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.entity.NotificationCode;
import com.project200.undabang.notification.fcm.dto.ChatNotificationContent;
import com.project200.undabang.notification.fcm.dto.ChatNotificationPayload;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import com.project200.undabang.notification.fcm.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatNotificationServiceImplTest {

    @InjectMocks
    private ChatNotificationServiceImpl chatNotificationService;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatroomMemberRepository chatroomMemberRepository;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private NotificationService notificationService;

    private Member createMember(UUID memberId) {
        return Member.builder()
                .memberId(memberId)
                .memberNickname("TestUser")
                .build();
    }

    private ChatNotificationContent createChatContent(Long chatId, Long chatroomId, UUID senderId, String content) {
        return new ChatNotificationContent(
                senderId,
                "SenderNick",
                chatroomId,
                content
        );
    }

    @Nested
    @DisplayName("sendChatNotification 메소드는")
    class Describe_sendChatNotification {

        @Test
        @DisplayName("성공: 모든 조건이 충족되면 알림 전송 서비스를 호출한다")
        void it_calls_notification_service_successfully() {
            // given
            Long chatId = 100L;
            Long chatroomId = 1L;
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();

            ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(chatId, chatroomId, senderId);

            ChatNotificationContent content = createChatContent(chatId, chatroomId, senderId, "안녕하세요");
            Member receiver = createMember(receiverId);
            List<String> tokens = List.of("token_1", "token_2");

            given(chatRepository.findChatContentForNotification(chatId))
                    .willReturn(Optional.of(content));

            given(chatroomMemberRepository.findOtherMemberInChatroom(chatroomId, senderId))
                    .willReturn(Optional.of(receiver));

            given(fcmTokenRepository.findAllActivatedFcmTokensForChat(receiverId))
                    .willReturn(tokens);

            // when
            chatNotificationService.sendChatNotification(event);

            // then
            ArgumentCaptor<ChatNotificationPayload> payloadCaptor = ArgumentCaptor.forClass(ChatNotificationPayload.class);
            ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);

            verify(notificationService).sendChatNotification(payloadCaptor.capture(), tokensCaptor.capture());

            assertThat(payloadCaptor.getValue().getContent()).isEqualTo("안녕하세요");
            assertThat(payloadCaptor.getValue().getType()).isEqualTo(NotificationCode.CHAT_MESSAGE.getCode());
            assertThat(tokensCaptor.getValue()).containsExactly("token_1", "token_2");
        }

        @Test
        @DisplayName("실패: 채팅 데이터를 찾을 수 없으면 예외를 발생시킨다")
        void it_throws_exception_when_chat_not_found() {
            // given
            ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(1L, 1L, UUID.randomUUID());

            given(chatRepository.findChatContentForNotification(event.chatId()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatNotificationService.sendChatNotification(event))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.CHAT_NOT_FOUND.getMessage());

            // 이후 로직은 실행되지 않아야 함
            verify(chatroomMemberRepository, never()).findOtherMemberInChatroom(any(), any());
            verify(notificationService, never()).sendChatNotification(any(), any());
        }

        @Test
        @DisplayName("실패: 수신자(상대방)를 찾을 수 없으면 예외를 발생시킨다")
        void it_throws_exception_when_receiver_not_found() {
            // given
            ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(1L, 1L, UUID.randomUUID());
            ChatNotificationContent content = createChatContent(1L, 1L, event.senderId(), "내용");

            given(chatRepository.findChatContentForNotification(event.chatId()))
                    .willReturn(Optional.of(content));

            given(chatroomMemberRepository.findOtherMemberInChatroom(event.chatroomId(), event.senderId()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatNotificationService.sendChatNotification(event))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage());

            verify(notificationService, never()).sendChatNotification(any(), any());
        }

        @Test
        @DisplayName("성공: 수신자의 활성 토큰이 없어도 에러 없이 빈 리스트로 호출한다 (혹은 정책에 따라 return)")
        void it_calls_service_even_if_tokens_empty() {
            // given
            UUID senderId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(1L, 1L, senderId);
            ChatNotificationContent content = createChatContent(1L, 1L, senderId, "내용");
            Member receiver = createMember(receiverId);

            given(chatRepository.findChatContentForNotification(event.chatId()))
                    .willReturn(Optional.of(content));
            given(chatroomMemberRepository.findOtherMemberInChatroom(event.chatroomId(), senderId))
                    .willReturn(Optional.of(receiver));

            // [핵심] 토큰이 없는 상황
            given(fcmTokenRepository.findAllActivatedFcmTokensForChat(receiverId))
                    .willReturn(Collections.emptyList());

            // when
            chatNotificationService.sendChatNotification(event);

            // then
            verify(notificationService).sendChatNotification(any(), eq(Collections.emptyList()));
        }
    }
}