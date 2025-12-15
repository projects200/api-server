package com.project200.undabang.notification.fcm.service.impl;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.*;
import com.project200.undabang.notification.fcm.dto.ChatNotificationPayload;
import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
import com.project200.undabang.notification.fcm.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

@Slf4j
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FcmNotificationService implements NotificationService {

    private final FcmTokenCommandService fcmTokenCommandService;
    private final FirebaseMessaging firebaseMessaging;
    private final Executor taskExecutor;

    @Autowired
    public FcmNotificationService(
            FcmTokenCommandService fcmTokenCommandService,
            FirebaseMessaging firebaseMessaging,
            @Qualifier("generalPurposeAsyncExecutor") Executor taskExecutor
    ) {
        this.fcmTokenCommandService = fcmTokenCommandService;
        this.firebaseMessaging = firebaseMessaging;
        this.taskExecutor = taskExecutor;
    }

    // 지정된 요청에 따라 단일 사용자에게 FCM 알림을 발송
    @Override
    public void sendNotification(NotificationPayload request) {
        // 1. 메시지 구성
        Message message = Message.builder()
                .setToken(request.targetUserToken())
                .setNotification(request.toNotification())
                .build();

        // 2. 로그 기록
        StringBuilder logMessage = new StringBuilder()
                .append("[알림 발송] FCM 단건 알림 발송을 비동기적으로 요청합니다. ")
                .append("To: ").append(request.targetUserToken());
        if (request.title() != null) logMessage.append(", Title: ").append(request.title());
        logMessage.append(", Body: ").append(request.body());
        if (request.imageUrl() != null) logMessage.append(", Image URL: ").append(request.imageUrl());
        log.info(logMessage.toString());

        // 3. 단일 메시지 비동기 발송
        ApiFuture<String> future = firebaseMessaging.sendAsync(message);

        // 4. 콜백 추가
        ApiFutures.addCallback(future, new ApiFutureCallback<>() {
            @Override
            public void onSuccess(String messageId) {
                log.info("[알림 발송 성공] FCM 단건 알림이 성공적으로 발송되었습니다. Message ID: {}", messageId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[알림 발송 실패] FCM 단건 알림 발송 중 오류 발생. Token: {}", request.targetUserToken(), t);

                if (t instanceof FirebaseMessagingException e) {
                    MessagingErrorCode errorCode = e.getMessagingErrorCode();
                    if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT || errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {
                        log.warn("[토큰 정리] 무효한 토큰으로 확인되어 삭제를 시도합니다: {}", request.targetUserToken());
                        fcmTokenCommandService.deleteInvalidTokens(List.of(request.targetUserToken()));
                    }
                }
            }
        }, taskExecutor);
    }

    // 지정된 요청 목록에 따라 다수의 사용자 FCM 알림을 발송
    @Override
    public void sendNotification(List<NotificationPayload> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        // 1. 메시지 구성
        List<Message> messages = requests.stream()
                .map(request -> Message.builder()
                        .setToken(request.targetUserToken())
                        .setNotification(request.toNotification())
                        .build())
                .toList();

        // 2. 로그 기록
        log.info("[알림 발송] FCM 다중 알림 발송을 비동기적으로 요청합니다. 총 {}건", messages.size());

        // 3. 다중 메시지 비동기 발송
        ApiFuture<BatchResponse> future = firebaseMessaging.sendEachAsync(messages);

        // 4. 콜백 추가
        ApiFutures.addCallback(future, new ApiFutureCallback<>() {
            @Override
            public void onSuccess(BatchResponse batchResponse) {
                log.info("[알림 발송 완료] 총 {}건 성공, {}건 실패",
                        batchResponse.getSuccessCount(),
                        batchResponse.getFailureCount());

                if (batchResponse.getFailureCount() > 0) {
                    List<String> failedTokens = new ArrayList<>();
                    List<SendResponse> responses = batchResponse.getResponses();

                    IntStream.range(0, responses.size())
                            .filter(i -> !responses.get(i).isSuccessful())
                            .forEach(i -> {
                                String failedToken = requests.get(i).targetUserToken();
                                failedTokens.add(failedToken);
                                FirebaseMessagingException e = responses.get(i).getException();
                                log.warn("[알림 발송 실패] 토큰: {}, 원인: {}, 코드: {}",
                                        failedToken, e.getMessage(), e.getMessagingErrorCode());
                            });

                    if (!failedTokens.isEmpty()) {
                        fcmTokenCommandService.deleteInvalidTokens(failedTokens);
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[알림 발송 실패] FCM 다중 발송 비동기 작업 중 심각한 오류 발생", t);
            }
        }, taskExecutor);
    }

    /**
     * 주어진 채팅 알림 내용을 FCM 토큰 목록에 따라 푸시 알림으로 발송합니다.
     */
    @Override
    public void sendChatNotification(ChatNotificationPayload payload, List<String> fcmTokenList) {

        if (fcmTokenList == null || fcmTokenList.isEmpty()) {
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(fcmTokenList)
                .putAllData(payload.toChatData())
                .build();

        ApiFuture<BatchResponse> response = firebaseMessaging.sendEachForMulticastAsync(message);

        ApiFutures.addCallback(response, new ApiFutureCallback<>() {
            @Override
            public void onSuccess(BatchResponse batchResponse) {
                log.info("[알림 발송 완료] 총 {}건 성공, {}건 실패",
                        batchResponse.getSuccessCount(),
                        batchResponse.getFailureCount());

                if (batchResponse.getFailureCount() > 0) {
                    List<String> failedTokens = new ArrayList<>();
                    List<SendResponse> responses = batchResponse.getResponses();

                    IntStream.range(0, responses.size())
                            .filter(i -> !responses.get(i).isSuccessful())
                            .forEach(i -> {
                                String failedToken = fcmTokenList.get(i);
                                failedTokens.add(failedToken);
                                FirebaseMessagingException e = responses.get(i).getException();
                                log.warn("[알림 발송 실패] 토큰: {}, 원인: {}, 코드: {}",
                                        failedToken, e.getMessage(), e.getMessagingErrorCode());
                            });

                    if (!failedTokens.isEmpty()) {
                        fcmTokenCommandService.deleteInvalidTokens(failedTokens);
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[채팅 알림 발송 실패] FCM 다중 발송 비동기 작업 중 심각한 오류 발생", t);
            }
        }, taskExecutor);
    }
}
