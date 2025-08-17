package com.project200.undabang.notification.fcm.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmNotificationService 클래스")
class FcmNotificationServiceTest {

    @InjectMocks
    private FcmNotificationService fcmNotificationService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Nested
    @DisplayName("sendNotification 메소드는")
    class Context_sendNotification {

        @Test
        @DisplayName("주어진 내용으로 FCM 알림을 성공적으로 발송한다")
        void sendNotification_success() throws FirebaseMessagingException {
            // given
            NotificationPayload payload = new NotificationPayload(
                    "test-fcm-token-12345",
                    "테스트 제목",
                    "테스트 본문입니다.",
                    "https://undabang.com/images/test.png"
            );

            // when
            fcmNotificationService.sendNotification(payload);

            // then
            // 1. Firebase Admin SDK의 Message 객체는 public getter를 제공하지 않아 내부 필드를 직접 검증할 수 없습니다.
            //    따라서 이 테스트의 핵심은 FcmNotificationService가 주어진 payload를 바탕으로
            //    FirebaseMessaging.send() 메소드를 '정확히 한 번' 호출하는 행위 자체를 검증하는 것입니다.
            //    Message 객체 생성 로직(내부 구현)은 FcmNotificationService 코드 리뷰를 통해 보장된다고 가정합니다.
            then(firebaseMessaging).should(times(1)).send(any(Message.class));
        }

        @Test
        @DisplayName("FCM 알림 발송 중 예외가 발생하면, 예외를 전파하지 않고 정상적으로 처리한다")
        void sendNotification_whenExceptionOccurs_shouldHandleGracefully() throws FirebaseMessagingException {
            // given
            NotificationPayload payload = new NotificationPayload(
                    "test-fcm-token-for-exception",
                    "예외 테스트",
                    "이 알림은 예외를 발생시킵니다.",
                    null
            );

            // 2. FirebaseMessagingException의 생성자가 public이 아니므로, new로 직접 생성하는 대신 Mock 객체를 만듭니다.
            FirebaseMessagingException fcmException = mock(FirebaseMessagingException.class);
            given(firebaseMessaging.send(any(Message.class)))
                    .willThrow(fcmException);

            // when & then
            // 3. 테스트 대상 메소드를 실행했을 때, 어떤 예외도 발생하지 않음을 검증합니다.
            //    이는 서비스 내부의 try-catch 블록이 예외를 잘 처리했음을 의미합니다.
            assertThatCode(() -> fcmNotificationService.sendNotification(payload))
                    .doesNotThrowAnyException();

            // 4. 예외가 발생했음에도 불구하고, send 메소드 호출 시도는 1번 이루어졌는지 확인합니다.
            then(firebaseMessaging).should(times(1)).send(any(Message.class));
        }
    }
}
