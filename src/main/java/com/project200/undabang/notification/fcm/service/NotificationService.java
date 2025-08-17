package com.project200.undabang.notification.fcm.service;

import com.project200.undabang.notification.fcm.dto.NotificationPayload;

/**
 * 알림 발송 기능을 정의하는 인터페이스입니다.
 */
public interface NotificationService {

    /**
     * 지정된 요청에 따라 단일 사용자에게 알림을 발송합니다.
     *
     * @param request 알림 발송에 필요한 정보 (타겟 토큰, 제목, 내용 등)
     */
    void sendNotification(NotificationPayload request);

}