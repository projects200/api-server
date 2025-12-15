package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.notification.fcm.dto.ChatNotificationPayload;
import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("LogNotificationService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class LogNotificationServiceTest {

    @InjectMocks
    private LogNotificationService logNotificationService;

    @Nested
    @DisplayName("sendNotification (단일 요청) 메소드는")
    class SendNotificationTest {

        @Test
        @DisplayName("모든 필드가 포함된 요청에 대해 예외 없이 성공한다")
        void givenAllFields_whenSendNotification_thenSucceeds() {
            // given
            NotificationPayload payload = new NotificationPayload(
                    "test-token-123",
                    "테스트 제목",
                    "테스트 본문입니다.",
                    "https://example.com/image.jpg"
            );

            // when & then
            assertDoesNotThrow(() -> logNotificationService.sendNotification(payload),
                    "모든 필드가 있을 때 예외가 발생해서는 안 됩니다.");
        }

        @Test
        @DisplayName("제목(title)이 없는 요청에 대해 예외 없이 성공한다")
        void givenPayloadWithoutTitle_whenSendNotification_thenSucceeds() {
            // given
            NotificationPayload payload = new NotificationPayload(
                    "test-token-123",
                    null,
                    "제목 없는 테스트 본문입니다.",
                    "https://example.com/image.jpg"
            );

            // when & then
            assertDoesNotThrow(() -> logNotificationService.sendNotification(payload),
                    "제목이 null일 때 예외가 발생해서는 안 됩니다.");
        }

        @Test
        @DisplayName("이미지 URL(imageUrl)이 없는 요청에 대해 예외 없이 성공한다")
        void givenPayloadWithoutImageUrl_whenSendNotification_thenSucceeds() {
            // given
            NotificationPayload payload = new NotificationPayload(
                    "test-token-123",
                    "이미지 없는 테스트",
                    "이미지 URL이 없는 테스트 본문입니다.",
                    null
            );

            // when & then
            assertDoesNotThrow(() -> logNotificationService.sendNotification(payload),
                    "이미지 URL이 null일 때 예외가 발생해서는 안 됩니다.");
        }

        @Test
        @DisplayName("필수 필드(token, body)만 있는 요청에 대해 예외 없이 성공한다")
        void givenPayloadWithRequiredFieldsOnly_whenSendNotification_thenSucceeds() {
            // given
            NotificationPayload payload = new NotificationPayload(
                    "test-token-123",
                    null,
                    "필수 필드만 있는 테스트 본문입니다.",
                    null
            );

            // when & then
            assertDoesNotThrow(() -> logNotificationService.sendNotification(payload),
                    "필수 필드만 있을 때 예외가 발생해서는 안 됩니다.");
        }
    }

    @Nested
    @DisplayName("sendNotification (다중 요청) 메소드는")
    class SendNotificationListTest {

        @Test
        @DisplayName("여러 개의 요청이 포함된 리스트에 대해 예외 없이 성공한다")
        void givenListOfPayloads_whenSendNotification_thenSucceeds() {
            // given
            List<NotificationPayload> payloads = List.of(
                    new NotificationPayload("token1", "제목1", "본문1", "url1"),
                    new NotificationPayload("token2", "제목2", "본문2", null)
            );

            // when & then
            assertDoesNotThrow(() -> logNotificationService.sendNotification(payloads),
                    "여러 요청을 포함한 리스트 처리 시 예외가 발생해서는 안 됩니다.");
        }

        @Test
        @DisplayName("비어 있는 리스트에 대해 예외 없이 성공한다")
        void givenEmptyList_whenSendNotification_thenSucceeds() {
            // given
            List<NotificationPayload> emptyList = Collections.emptyList();

            // when & then
            assertDoesNotThrow(() -> logNotificationService.sendNotification(emptyList),
                    "비어있는 리스트 처리 시 예외가 발생해서는 안 됩니다.");
        }

        @Test
        @DisplayName("null 요소를 포함한 리스트에 대해 예외 없이 성공한다")
        void givenListWithNullElement_whenSendNotification_thenSucceeds() {
            // given
            List<NotificationPayload> payloads = Arrays.asList(
                    new NotificationPayload("token1", "제목1", "본문1", "url1"),
                    null // 두 번째 항목이 null이지만, 현재 로직에서는 접근하지 않음
            );

            // when & then
            assertDoesNotThrow(() -> logNotificationService.sendNotification(payloads),
                    "리스트의 일부 요소가 null이더라도 첫 번째 요소만 접근하므로 예외가 발생해서는 안 됩니다.");
        }
    }

    @Nested
    @DisplayName("sendChatNotification (채팅 알림) 메소드는")
    class SendChatNotificationTest {

        @Test
        @DisplayName("정상적인 입력값(Payload와 토큰 리스트)에 대해 예외 없이 성공한다")
        void givenValidPayloadAndTokens_whenSendChatNotification_thenSucceeds() {
            // given
            ChatNotificationPayload payload = ChatNotificationPayload.builder()
                    .type("CHAT_MESSAGE")
                    .chatroomId(1L)
                    .memberId(UUID.randomUUID())
                    .nickname("Tester")
                    .content("Hello")
                    .build();

            List<String> tokens = List.of("token1", "token2");

            // when & then
            assertDoesNotThrow(() -> logNotificationService.sendChatNotification(payload, tokens),
                    "정상적인 입력값에 대해 예외가 발생해서는 안 됩니다.");
        }

        @Test
        @DisplayName("토큰 리스트가 비어 있어도 예외 없이 성공한다")
        void givenEmptyTokenList_whenSendChatNotification_thenSucceeds() {
            // given
            ChatNotificationPayload payload = ChatNotificationPayload.builder()
                    .type("CHAT_MESSAGE")
                    .chatroomId(1L)
                    .memberId(UUID.randomUUID())
                    .nickname("Tester")
                    .content("Hello")
                    .build();

            List<String> emptyTokens = Collections.emptyList();

            // when & then
            assertDoesNotThrow(() -> logNotificationService.sendChatNotification(payload, emptyTokens),
                    "토큰 리스트가 비어 있어도 예외가 발생해서는 안 됩니다.");
        }

        @Test
        @DisplayName("토큰 리스트가 null이어도 예외 없이 성공한다")
        void givenNullTokenList_whenSendChatNotification_thenSucceeds() {
            // given
            ChatNotificationPayload payload = ChatNotificationPayload.builder()
                    .type("CHAT_MESSAGE")
                    .chatroomId(1L)
                    .memberId(UUID.randomUUID())
                    .nickname("Tester")
                    .content("Hello")
                    .build();

            // when & then
            assertDoesNotThrow(() -> logNotificationService.sendChatNotification(payload, null),
                    "토큰 리스트가 null이어도 예외가 발생해서는 안 됩니다.");
        }
    }
}