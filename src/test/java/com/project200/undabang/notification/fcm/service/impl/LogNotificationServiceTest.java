package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("LogNotificationService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class LogNotificationServiceTest {

    @InjectMocks
    private LogNotificationService logNotificationService;

    @Nested
    @DisplayName("sendNotification 메소드는")
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
}