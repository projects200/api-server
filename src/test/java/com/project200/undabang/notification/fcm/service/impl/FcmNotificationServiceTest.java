package com.project200.undabang.notification.fcm.service.impl;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.*;
import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.*;

@DisplayName("FcmNotificationService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class FcmNotificationServiceTest {

    // @InjectMocks를 사용하지 않고 수동으로 주입하여 Executor를 제어합니다.
    private FcmNotificationService fcmNotificationService;

    @Mock
    private FcmTokenCommandService fcmTokenCommandService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Captor
    private ArgumentCaptor<List<String>> tokenListCaptor;

    @BeforeEach
    void setUp() {
        // 비동기 콜백을 테스트 스레드에서 즉시 실행하는 Executor를 사용합니다.
        Executor sameThreadExecutor = Runnable::run;
        fcmNotificationService = new FcmNotificationService(fcmTokenCommandService, firebaseMessaging, sameThreadExecutor);
    }

    private NotificationPayload createNotificationPayload(String token, String title, String body) {
        return new NotificationPayload(token, title, body, null);
    }

    /**
     * Mockito와 커버리지 도구의 충돌을 피하기 위한 BatchResponse의 테스트용 구현체
     */
    private static class TestBatchResponse implements BatchResponse {
        private final List<SendResponse> responses;
        private final int successCount;
        private final int failureCount;

        TestBatchResponse(List<SendResponse> responses) {
            this.responses = responses;
            this.successCount = (int) responses.stream().filter(SendResponse::isSuccessful).count();
            this.failureCount = responses.size() - successCount;
        }

        @Override
        public int getSuccessCount() {
            return successCount;
        }

        @Override
        public int getFailureCount() {
            return failureCount;
        }

        @Override
        public List<SendResponse> getResponses() {
            return responses;
        }
    }

    // --- Test Helper Methods ---

    @Nested
    @DisplayName("sendNotification(단건) 메소드는")
    class SendNotificationSingle {

        @Test
        @DisplayName("성공적으로 단건 알림을 보내고, 성공 콜백을 호출한다")
        void sendNotification_onSuccess() {
            // given
            NotificationPayload payload = createNotificationPayload("token1", "제목1", "내용1");
            String messageId = "projects/undabang/messages/12345";

            // 즉시 성공하는 ApiFuture를 생성합니다.
            ApiFuture<String> successFuture = ApiFutures.immediateFuture(messageId);
            given(firebaseMessaging.sendAsync(any(Message.class))).willReturn(successFuture);

            // when
            fcmNotificationService.sendNotification(payload);

            // then
            // 콜백이 동기적으로 실행되었으므로, 토큰 삭제 로직이 호출되지 않았음을 바로 검증할 수 있습니다.
            then(fcmTokenCommandService).should(never()).deleteInvalidTokens(any());
        }

        @Test
        @DisplayName("알림 발송 실패 시 UNREGISTERED 에러코드이면 토큰 삭제를 시도한다")
        void sendNotification_onFailure_withUnregisteredToken() {
            // given
            NotificationPayload payload = createNotificationPayload("invalid-token", "제목", "내용");
            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);

            // 즉시 실패하는 ApiFuture를 생성합니다.
            ApiFuture<String> failedFuture = ApiFutures.immediateFailedFuture(exception);
            given(firebaseMessaging.sendAsync(any(Message.class))).willReturn(failedFuture);

            // when
            fcmNotificationService.sendNotification(payload);

            // then
            // onFailure 콜백이 동기적으로 실행되었으므로, 토큰 삭제 로직이 호출되었는지 검증합니다.
            then(fcmTokenCommandService).should(times(1)).deleteInvalidTokens(List.of(payload.targetUserToken()));
        }

        @Test
        @DisplayName("알림 발송 실패 시 INVALID_ARGUMENT 에러코드이면 토큰 삭제를 시도한다")
        void sendNotification_onFailure_withInvalidArgument() {
            // given
            NotificationPayload payload = createNotificationPayload("invalid-token", "제목", "내용");
            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.INVALID_ARGUMENT);
            ApiFuture<String> failedFuture = ApiFutures.immediateFailedFuture(exception);
            given(firebaseMessaging.sendAsync(any(Message.class))).willReturn(failedFuture);

            // when
            fcmNotificationService.sendNotification(payload);

            // then
            then(fcmTokenCommandService).should(times(1)).deleteInvalidTokens(List.of(payload.targetUserToken()));
        }

        @Test
        @DisplayName("알림 발송 실패 시 SENDER_ID_MISMATCH 에러코드이면 토큰 삭제를 시도한다")
        void sendNotification_onFailure_withSenderIdMismatch() {
            // given
            NotificationPayload payload = createNotificationPayload("invalid-token", "제목", "내용");
            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.SENDER_ID_MISMATCH);
            ApiFuture<String> failedFuture = ApiFutures.immediateFailedFuture(exception);
            given(firebaseMessaging.sendAsync(any(Message.class))).willReturn(failedFuture);

            // when
            fcmNotificationService.sendNotification(payload);

            // then
            then(fcmTokenCommandService).should(times(1)).deleteInvalidTokens(List.of(payload.targetUserToken()));
        }

        @Test
        @DisplayName("알림 발송 실패 시 INTERNAL 에러코드이면 토큰을 삭제하지 않는다")
        void sendNotification_onFailure_withOtherError() {
            // given
            NotificationPayload payload = createNotificationPayload("some-token", "제목", "내용");
            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.INTERNAL);
            ApiFuture<String> failedFuture = ApiFutures.immediateFailedFuture(exception);
            given(firebaseMessaging.sendAsync(any(Message.class))).willReturn(failedFuture);

            // when
            fcmNotificationService.sendNotification(payload);

            // then
            then(fcmTokenCommandService).should(never()).deleteInvalidTokens(any());
        }
    }

    @Nested
    @DisplayName("sendNotification(다건) 메소드는")
    class SendNotificationBatch {

        @Test
        @DisplayName("빈 리스트가 주어지면 아무 동작도 하지 않는다")
        void sendNotification_withEmptyList() {
            // given
            List<NotificationPayload> emptyList = Collections.emptyList();

            // when
            fcmNotificationService.sendNotification(emptyList);

            // then
            then(firebaseMessaging).should(never()).sendEachAsync(anyList());
        }

        @Test
        @DisplayName("모든 알림이 성공적으로 발송되면 성공 콜백을 호출하고 토큰을 삭제하지 않는다")
        void sendNotificationBatch_allSuccess() {
            // given
            List<NotificationPayload> payloads = List.of(
                    createNotificationPayload("token1", "제목1", "내용1"),
                    createNotificationPayload("token2", "제목2", "내용2")
            );
            // BatchResponse를 모킹하는 대신, 테스트용 구현체를 사용합니다.
            SendResponse successResponse1 = mock(SendResponse.class);
            given(successResponse1.isSuccessful()).willReturn(true);
            SendResponse successResponse2 = mock(SendResponse.class);
            given(successResponse2.isSuccessful()).willReturn(true);

            BatchResponse testBatchResponse = new TestBatchResponse(List.of(successResponse1, successResponse2));

            ApiFuture<BatchResponse> successFuture = ApiFutures.immediateFuture(testBatchResponse);
            given(firebaseMessaging.sendEachAsync(anyList())).willReturn(successFuture);

            // when
            fcmNotificationService.sendNotification(payloads);

            // then
            then(fcmTokenCommandService).should(never()).deleteInvalidTokens(any());
        }

        @Test
        @DisplayName("일부 알림이 실패하면 실패한 토큰들만 삭제를 시도한다")
        void sendNotificationBatch_partialFailure() {
            // given
            NotificationPayload successPayload = createNotificationPayload("token-success", "성공", "성공 내용");
            NotificationPayload failurePayload = createNotificationPayload("token-failure", "실패", "실패 내용");
            List<NotificationPayload> payloads = List.of(successPayload, failurePayload);

            SendResponse successResponse = mock(SendResponse.class);
            given(successResponse.isSuccessful()).willReturn(true);

            SendResponse failureResponse = mock(SendResponse.class);
            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            given(failureResponse.isSuccessful()).willReturn(false);
            given(failureResponse.getException()).willReturn(exception);
            given(exception.getMessage()).willReturn("Invalid token");
            given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.INVALID_ARGUMENT);

            // BatchResponse를 모킹하는 대신, 테스트용 구현체를 사용합니다.
            BatchResponse testBatchResponse = new TestBatchResponse(List.of(successResponse, failureResponse));

            ApiFuture<BatchResponse> partialFailureFuture = ApiFutures.immediateFuture(testBatchResponse);
            given(firebaseMessaging.sendEachAsync(anyList())).willReturn(partialFailureFuture);

            // when
            fcmNotificationService.sendNotification(payloads);

            // then
            then(fcmTokenCommandService).should(times(1)).deleteInvalidTokens(tokenListCaptor.capture());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(tokenListCaptor.getValue()).as("삭제 요청 토큰 리스트 검증")
                        .containsExactly(failurePayload.targetUserToken());
            });
        }

        @Test
        @DisplayName("비동기 작업 자체가 실패하면 onFailure 콜백을 호출한다")
        void sendNotificationBatch_futureFails() {
            // given
            List<NotificationPayload> payloads = List.of(
                    createNotificationPayload("token1", "제목1", "내용1")
            );
            RuntimeException exception = new RuntimeException("심각한 오류 발생");
            ApiFuture<BatchResponse> failedFuture = ApiFutures.immediateFailedFuture(exception);
            given(firebaseMessaging.sendEachAsync(anyList())).willReturn(failedFuture);

            // when
            fcmNotificationService.sendNotification(payloads);

            // then
            then(fcmTokenCommandService).should(never()).deleteInvalidTokens(any());
        }
    }
}
