package com.project200.undabang.notification.fcm.service;

/**
 * 알림 발송을 하는 서비스 입니다.
 */
public interface NotificationBatchService {

    /**
     * 활동이 없는 사용자들에게 알림을 발송합니다.
     * <p>
     * 활동 기준에 따라 비활성 사용자들을 식별한 후, 이들에게 알림을 전송하는 메서드입니다.
     * 이는 사용자가 활동을 재개하도록 유도하는 데 목적이 있습니다.
     */
    void sendInactivityNotifications();
}
