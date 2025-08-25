package com.project200.undabang.notification.fcm.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import com.project200.undabang.notification.fcm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FcmNotificationService implements NotificationService {

    private final FirebaseMessaging firebaseMessaging;

    // 지정된 요청에 따라 단일 사용자에게 FCM 알림을 발송
    @Override
    public void sendNotification(NotificationPayload request) {
        Message message = Message.builder()
                .setToken(request.targetUserToken())
                .setNotification(request.toNotification())
                .build();

        StringBuilder logMessage = new StringBuilder()
                .append("[알림 발송] FCM 알림 발송을 시도합니다. ")
                .append("To: ").append(request.targetUserToken());
        if (request.title() != null) logMessage.append(", Title: ").append(request.title());
        logMessage.append(", Body: ").append(request.body());
        if (request.imageUrl() != null) logMessage.append(", Image URL: ").append(request.imageUrl());
        log.info(logMessage.toString());
        firebaseMessaging.sendAsync(message);
        log.info("[알림 발송] FCM 알림 발송에 성공했습니다.");
    }

    // 지정된 요청 목록에 따라 다수의 사용자 FCM 알림을 발송
    @Override
    public void sendNotification(List<NotificationPayload> requests) {
        List<Message> messages = requests.stream()
                .map(request -> Message.builder()
                        .setToken(request.targetUserToken())
                        .setNotification(request.toNotification())
                        .build())
                .toList();

        String logMessage = "[알림 발송] FCM 알림 발송을 시도합니다. " +
                "총 " + requests.size() + "건의 알림 발송 요청이 있습니다.";
        log.info(logMessage);
        firebaseMessaging.sendEachAsync(messages);
        log.info("[알림 발송] FCM 알림 발송에 성공했습니다.");
    }
}
