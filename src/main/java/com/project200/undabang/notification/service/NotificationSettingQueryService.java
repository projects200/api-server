package com.project200.undabang.notification.service;

import com.project200.undabang.notification.dto.response.GetAllDeviceNotificationSettingsResponse;

import java.util.List;

public interface NotificationSettingQueryService {
    List<GetAllDeviceNotificationSettingsResponse> getAllDeviceNotificationSettings(String fcmToken);
}
