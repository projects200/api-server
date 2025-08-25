package com.project200.undabang.notification.fcm.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.project200.undabang.notification.fcm.dto.NotificationPayload;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmNotificationService 클래스")
class FcmNotificationServiceTest {

    @InjectMocks
    private FcmNotificationService fcmNotificationService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Nested
    @DisplayName("sendNotification (단일 요청) 메소드는")
    class Context_sendNotification {

        @Test
        @DisplayName("주어진 내용으로 FCM 알림을 성공적으로 발송한다")
        void sendNotification_success() {
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
            // 비동기 메서드인 sendAsync가 호출되는지 검증하도록 수정
            then(firebaseMessaging).should(times(1)).sendAsync(any(Message.class));
        }
    }

    @Nested
    @DisplayName("sendNotification (다중 요청) 메소드는")
    class Context_sendNotification_List {

        @Test
        @DisplayName("주어진 내용 리스트로 FCM 알림을 성공적으로 발송한다")
        void sendNotification_withList_success() {
            // given
            List<NotificationPayload> payloads = List.of(
                    new NotificationPayload("token1", "제목1", "본문1", "url1"),
                    new NotificationPayload("token2", "제목2", "본문2", null)
            );

            // when
            fcmNotificationService.sendNotification(payloads);

            // then
            // ArgumentCaptor를 사용하여 sendEachAsync에 전달된 인자를 캡처
            ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
            then(firebaseMessaging).should(times(1)).sendEachAsync(captor.capture());

            // 캡처된 리스트의 크기가 올바른지 확인
            List<Message> capturedMessages = captor.getValue();
            assertThat(capturedMessages).hasSize(payloads.size());
        }

        @Test
        @DisplayName("비어있는 리스트가 주어져도 예외 없이 정상 처리된다")
        void sendNotification_withEmptyList_shouldHandleGracefully() {
            // given
            List<NotificationPayload> emptyPayloads = Collections.emptyList();

            // when
            fcmNotificationService.sendNotification(emptyPayloads);

            // then
            ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
            then(firebaseMessaging).should(times(1)).sendEachAsync(captor.capture());

            // 캡처된 리스트가 비어있는지 확인
            List<Message> capturedMessages = captor.getValue();
            assertThat(capturedMessages).isEmpty();
        }
    }
}
