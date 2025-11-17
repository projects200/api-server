package com.project200.undabang.notification.fcm.repository;

import com.project200.undabang.notification.fcm.entity.FcmToken;

public interface DeviceNotificationSettingRepositoryCustom {
    void deleteAllByFcmToken(FcmToken fcmToken);
}
