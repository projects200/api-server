package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import com.project200.undabang.notification.fcm.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
public class LogNotificationService implements NotificationService {

    @Override
    public void sendNotification(NotificationPayload request) {
        StringBuilder logMessage = new StringBuilder()
                .append("[알림 발송] FCM 기능이 비활성화 상태입니다. ")
                .append("To: ").append(request.targetUserToken());
        if (request.title() != null) logMessage.append(", Title: ").append(request.title());
        logMessage.append(", Body: ").append(request.body());
        if (request.imageUrl() != null) logMessage.append(", Image URL: ").append(request.imageUrl());

        log.info(logMessage.toString());
    }
}
