package com.project200.undabang.notification.fcm.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import com.project200.undabang.notification.fcm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FcmNotificationService implements NotificationService {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void sendNotification(NotificationPayload request) {
        Notification notification = Notification.builder()
                .setTitle(request.title())
                .setBody(request.body())
                .setImage(request.imageUrl())
                .build();

        Message message = Message.builder()
                .setToken(request.targetUserToken())
                .setNotification(notification)
                .build();

        try {
            StringBuilder logMessage = new StringBuilder()
                    .append("[알림 발송] FCM 알림 발송을 시도합니다. ")
                    .append("To: ").append(request.targetUserToken());
            if (request.title() != null) logMessage.append(", Title: ").append(request.title());
            logMessage.append(", Body: ").append(request.body());
            if (request.imageUrl() != null) logMessage.append(", Image URL: ").append(request.imageUrl());
            log.info(logMessage.toString());
            firebaseMessaging.send(message);
            log.info("[알림 발송] FCM 알림 발송에 성공했습니다.");
        } catch (FirebaseMessagingException e) {
            log.error("[알림 발송] FCM 알림 발송에 실패했습니다. (To: {})", request.targetUserToken(), e);
        }
    }
}
