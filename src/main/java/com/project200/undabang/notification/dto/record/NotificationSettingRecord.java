package com.project200.undabang.notification.dto.record;

import com.project200.undabang.notification.fcm.entity.NotificationType;

public record NotificationSettingRecord(NotificationType type, boolean enabled) {
}
