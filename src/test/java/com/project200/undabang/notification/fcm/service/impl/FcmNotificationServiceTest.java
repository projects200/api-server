package com.project200.undabang.notification.fcm.service.impl;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.*;
import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("FcmNotificationService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class FcmNotificationServiceTest {

    @InjectMocks
    private FcmNotificationService fcmNotificationService;

    @Mock
    private FcmTokenCommandService fcmTokenCommandService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private Executor taskExecutor;

    @Captor
    private ArgumentCaptor<ApiFutureCallback<String>> singleSendCallbackCaptor;

    @Captor
    private ArgumentCaptor<ApiFutureCallback<BatchResponse>> batchSendCallbackCaptor;

    @Captor
    private ArgumentCaptor<List<String>> tokenListCaptor;

    private NotificationPayload createNotificationPayload(String token, String title, String body) {
        return new NotificationPayload(token, title, body, null);
    }

    @Nested
    @DisplayName("sendNotification(단건) 메소드는")
    class SendNotificationSingle {

        @Test
        @DisplayName("성공적으로 단건 알림을 보내고, 성공 콜백을 호출한다")
        void sendNotification_onSuccess() {
            // given
            try (MockedStatic<ApiFutures> mockedApiFutures = mockStatic(ApiFutures.class)) {
                NotificationPayload payload = createNotificationPayload("token1", "제목1", "내용1");
                ApiFuture<String> mockApiFuture = mock(ApiFuture.class);
                String messageId = "projects/undabang/messages/12345";

                given(firebaseMessaging.sendAsync(any(Message.class))).willReturn(mockApiFuture);

                // when
                fcmNotificationService.sendNotification(payload);

                // then
                // ApiFutures.addCallback 정적 메소드가 호출되었는지 검증하고, 콜백 인스턴스를 캡처
                mockedApiFutures.verify(() -> ApiFutures.addCallback(
                        eq(mockApiFuture),
                        singleSendCallbackCaptor.capture(),
                        eq(taskExecutor)
                ));

                // 캡처된 콜백의 onSuccess를 수동으로 호출하여 비동기 성공 상황을 시뮬레이션
                ApiFutureCallback<String> capturedCallback = singleSendCallbackCaptor.getValue();
                capturedCallback.onSuccess(messageId);

                // 성공 시 토큰 삭제 로직은 호출되지 않음을 검증
                then(fcmTokenCommandService).should(never()).deleteInvalidTokens(any());
            }
        }

        @Test
        @DisplayName("알림 발송 실패 시 UNREGISTERED 에러코드이면 토큰 삭제를 시도한다")
        void sendNotification_onFailure_withUnregisteredToken() {
            // given
            try (MockedStatic<ApiFutures> mockedApiFutures = mockStatic(ApiFutures.class)) {
                NotificationPayload payload = createNotificationPayload("invalid-token", "제목", "내용");
                ApiFuture<String> mockApiFuture = mock(ApiFuture.class);
                FirebaseMessagingException exception = mock(FirebaseMessagingException.class);

                given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);
                given(firebaseMessaging.sendAsync(any(Message.class))).willReturn(mockApiFuture);

                // when
                fcmNotificationService.sendNotification(payload);

                // then
                mockedApiFutures.verify(() -> ApiFutures.addCallback(
                        eq(mockApiFuture),
                        singleSendCallbackCaptor.capture(),
                        eq(taskExecutor)
                ));

                // 캡처된 콜백의 onFailure를 수동으로 호출하여 비동기 실패 상황을 시뮬레이션
                ApiFutureCallback<String> capturedCallback = singleSendCallbackCaptor.getValue();
                capturedCallback.onFailure(exception);

                // UNREGISTERED 에러이므로 토큰 삭제 로직이 호출되어야 함
                then(fcmTokenCommandService).should(times(1)).deleteInvalidTokens(List.of(payload.targetUserToken()));
            }
        }

        @Test
        @DisplayName("알림 발송 실패 시 INVALID_ARGUMENT 에러코드이면 토큰 삭제를 시도한다")
        void sendNotification_onFailure_withInvalidArgument() {
            // given
            try (MockedStatic<ApiFutures> mockedApiFutures = mockStatic(ApiFutures.class)) {
                NotificationPayload payload = createNotificationPayload("invalid-token", "제목", "내용");
                ApiFuture<String> mockApiFuture = mock(ApiFuture.class);
                FirebaseMessagingException exception = mock(FirebaseMessagingException.class);

                given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.INVALID_ARGUMENT);
                given(firebaseMessaging.sendAsync(any(Message.class))).willReturn(mockApiFuture);

                // when
                fcmNotificationService.sendNotification(payload);

                // then
                mockedApiFutures.verify(() -> ApiFutures.addCallback(
                        eq(mockApiFuture),
                        singleSendCallbackCaptor.capture(),
                        eq(taskExecutor)
                ));

                // 캡처된 콜백의 onFailure를 수동으로 호출
                singleSendCallbackCaptor.getValue().onFailure(exception);

                // INVALID_ARGUMENT 에러이므로 토큰 삭제 로직이 호출되어야 함
                then(fcmTokenCommandService).should(times(1)).deleteInvalidTokens(List.of(payload.targetUserToken()));
            }
        }

        @Test
        @DisplayName("알림 발송 실패 시 SENDER_ID_MISMATCH 에러코드이면 토큰 삭제를 시도한다")
        void sendNotification_onFailure_withSenderIdMismatch() {
            // given
            try (MockedStatic<ApiFutures> mockedApiFutures = mockStatic(ApiFutures.class)) {
                NotificationPayload payload = createNotificationPayload("invalid-token", "제목", "내용");
                ApiFuture<String> mockApiFuture = mock(ApiFuture.class);
                FirebaseMessagingException exception = mock(FirebaseMessagingException.class);

                given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.SENDER_ID_MISMATCH);
                given(firebaseMessaging.sendAsync(any(Message.class))).willReturn(mockApiFuture);

                // when
                fcmNotificationService.sendNotification(payload);

                // then
                mockedApiFutures.verify(() -> ApiFutures.addCallback(
                        eq(mockApiFuture),
                        singleSendCallbackCaptor.capture(),
                        eq(taskExecutor)
                ));

                // 캡처된 콜백의 onFailure를 수동으로 호출
                singleSendCallbackCaptor.getValue().onFailure(exception);

                // SENDER_ID_MISMATCH 에러이므로 토큰 삭제 로직이 호출되어야 함
                then(fcmTokenCommandService).should(times(1)).deleteInvalidTokens(List.of(payload.targetUserToken()));
            }
        }

        @Test
        @DisplayName("알림 발송 실패 시 INTERNAL 에러코드이면 토큰을 삭제하지 않는다")
        void sendNotification_onFailure_withOtherError() {
            // given
            try (MockedStatic<ApiFutures> mockedApiFutures = mockStatic(ApiFutures.class)) {
                NotificationPayload payload = createNotificationPayload("some-token", "제목", "내용");
                ApiFuture<String> mockApiFuture = mock(ApiFuture.class);
                FirebaseMessagingException exception = mock(FirebaseMessagingException.class);

                given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.INTERNAL);
                given(firebaseMessaging.sendAsync(any(Message.class))).willReturn(mockApiFuture);

                // when
                fcmNotificationService.sendNotification(payload);

                // then
                mockedApiFutures.verify(() -> ApiFutures.addCallback(
                        eq(mockApiFuture),
                        singleSendCallbackCaptor.capture(),
                        eq(taskExecutor)
                ));

                // 캡처된 콜백의 onFailure를 수동으로 호출
                ApiFutureCallback<String> capturedCallback = singleSendCallbackCaptor.getValue();
                capturedCallback.onFailure(exception);

                // INTERNAL 에러이므로 토큰 삭제 로직이 호출되지 않아야 함
                then(fcmTokenCommandService).should(never()).deleteInvalidTokens(any());
            }
        }
    }


    // --- Test Helper Methods ---

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
            try (MockedStatic<ApiFutures> mockedApiFutures = mockStatic(ApiFutures.class)) {
                List<NotificationPayload> payloads = List.of(
                        createNotificationPayload("token1", "제목1", "내용1"),
                        createNotificationPayload("token2", "제목2", "내용2")
                );
                ApiFuture<BatchResponse> mockApiFuture = mock(ApiFuture.class);
                BatchResponse mockBatchResponse = mock(BatchResponse.class);

                given(firebaseMessaging.sendEachAsync(anyList())).willReturn(mockApiFuture);
                given(mockBatchResponse.getSuccessCount()).willReturn(2);
                given(mockBatchResponse.getFailureCount()).willReturn(0);

                // when
                fcmNotificationService.sendNotification(payloads);

                // then
                mockedApiFutures.verify(() -> ApiFutures.addCallback(
                        eq(mockApiFuture),
                        batchSendCallbackCaptor.capture(),
                        eq(taskExecutor)
                ));

                // 캡처된 콜백을 수동으로 실행
                batchSendCallbackCaptor.getValue().onSuccess(mockBatchResponse);

                // 실패가 없으므로 토큰 삭제는 호출되지 않음
                then(fcmTokenCommandService).should(never()).deleteInvalidTokens(any());
            }
        }

        @Test
        @DisplayName("일부 알림이 실패하면 실패한 토큰들만 삭제를 시도한다")
        void sendNotificationBatch_partialFailure() {
            // given
            try (MockedStatic<ApiFutures> mockedApiFutures = mockStatic(ApiFutures.class)) {
                NotificationPayload successPayload = createNotificationPayload("token-success", "성공", "성공 내용");
                NotificationPayload failurePayload = createNotificationPayload("token-failure", "실패", "실패 내용");
                List<NotificationPayload> payloads = List.of(successPayload, failurePayload);

                ApiFuture<BatchResponse> mockApiFuture = mock(ApiFuture.class);
                BatchResponse mockBatchResponse = mock(BatchResponse.class);
                SendResponse successResponse = mock(SendResponse.class);
                SendResponse failureResponse = mock(SendResponse.class);
                FirebaseMessagingException exception = mock(FirebaseMessagingException.class);

                given(exception.getMessage()).willReturn("Invalid token");
                given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.INVALID_ARGUMENT);
                given(firebaseMessaging.sendEachAsync(anyList())).willReturn(mockApiFuture);
                given(mockBatchResponse.getFailureCount()).willReturn(1);
                given(mockBatchResponse.getSuccessCount()).willReturn(1);
                given(mockBatchResponse.getResponses()).willReturn(List.of(successResponse, failureResponse));
                given(successResponse.isSuccessful()).willReturn(true);
                given(failureResponse.isSuccessful()).willReturn(false);
                given(failureResponse.getException()).willReturn(exception);

                // when
                fcmNotificationService.sendNotification(payloads);

                // then
                mockedApiFutures.verify(() -> ApiFutures.addCallback(
                        eq(mockApiFuture),
                        batchSendCallbackCaptor.capture(),
                        eq(taskExecutor)
                ));

                // 캡처된 콜백을 수동으로 실행
                batchSendCallbackCaptor.getValue().onSuccess(mockBatchResponse);

                // 실패한 토큰만 삭제하도록 요청되었는지 검증
                then(fcmTokenCommandService).should(times(1)).deleteInvalidTokens(tokenListCaptor.capture());
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(tokenListCaptor.getValue()).as("삭제 요청 토큰 리스트 검증")
                            .containsExactly(failurePayload.targetUserToken());
                });
            }
        }

        @Test
        @DisplayName("비동기 작업 자체가 실패하면 onFailure 콜백을 호출한다")
        void sendNotificationBatch_futureFails() {
            // given
            try (MockedStatic<ApiFutures> mockedApiFutures = mockStatic(ApiFutures.class)) {
                List<NotificationPayload> payloads = List.of(
                        createNotificationPayload("token1", "제목1", "내용1")
                );
                ApiFuture<BatchResponse> mockApiFuture = mock(ApiFuture.class);
                RuntimeException exception = new RuntimeException("심각한 오류 발생");
                given(firebaseMessaging.sendEachAsync(anyList())).willReturn(mockApiFuture);

                // when
                fcmNotificationService.sendNotification(payloads);

                // then
                mockedApiFutures.verify(() -> ApiFutures.addCallback(
                        eq(mockApiFuture),
                        batchSendCallbackCaptor.capture(),
                        eq(taskExecutor)
                ));

                // 캡처된 콜백을 수동으로 실행
                batchSendCallbackCaptor.getValue().onFailure(exception);

                // 작업 자체가 실패했으므로 개별 토큰 삭제 로직은 타지 않음
                then(fcmTokenCommandService).should(never()).deleteInvalidTokens(any());
            }
        }
    }
}
