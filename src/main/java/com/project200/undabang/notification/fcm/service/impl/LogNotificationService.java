package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.notification.fcm.dto.ChatNotificationPayload;
import com.project200.undabang.notification.fcm.dto.NotificationPayload;
import com.project200.undabang.notification.fcm.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
public class LogNotificationService implements NotificationService {

    // 지정된 요청에 따라 단일 사용자에게 알림을 발송한다는 로그를 작성
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

    // 지정된 요청 목록에 따라 다수의 사용자에게 알림을 발송한다는 로그 작성
    @Override
    public void sendNotification(List<NotificationPayload> requests) {
        StringBuilder logMessage = new StringBuilder()
                .append("[알림 발송] FCM 기능이 비활성화 상태입니다. ")
                .append("총 ").append(requests.size()).append("건의 알림 발송 요청이 있습니다.");

        // 첫 번째 요청의 상세 정보만 로그에 남깁니다.
        if (!requests.isEmpty()) {
            logMessage.append(" 첫 번째 알림: ");
            NotificationPayload first = requests.getFirst();
            logMessage.append("To: ").append(first.targetUserToken());
            if (first.title() != null) logMessage.append(", Title: ").append(first.title());
            logMessage.append(", Body: ").append(first.body());
            if (first.imageUrl() != null) logMessage.append(", Image URL: ").append(first.imageUrl());
        }
        log.info(logMessage.toString());
    }

    // 채팅 메시지 생성시 FCM 토큰 내용과 전송될 토큰의 수를 출력하는 로그 작성
    @Override
    public void sendChatNotification(ChatNotificationPayload payload, List<String> fcmTokenList) {
        StringBuilder logMessage = new StringBuilder();

        logMessage.append("\n[채팅 알림 발송 - Mock] FCM 기능이 비활성화 상태입니다.");
        logMessage.append("\n==================================================");
        logMessage.append("\n 1. 수신 대상 정보");
        logMessage.append("\n   - 대상 토큰 수 : ").append(fcmTokenList != null ? fcmTokenList.size() : 0).append("개");
        if (fcmTokenList != null && !fcmTokenList.isEmpty()) {
            logMessage.append("\n   - 첫 번째 토큰 : ").append(fcmTokenList.get(0)).append("...");
        }

        logMessage.append("\n 2. 전송 데이터 (Payload)");
        logMessage.append("\n   - Type       : ").append(payload.getType());
        logMessage.append("\n   - ChatroomID : ").append(payload.getChatroomId());
        logMessage.append("\n   - MemberID   : ").append(payload.getMemberId());
        logMessage.append("\n   - Nickname   : ").append(payload.getNickname());
        logMessage.append("\n   - Content    : ").append(payload.getContent());
        logMessage.append("\n==================================================");

        log.info(logMessage.toString());
    }
}
