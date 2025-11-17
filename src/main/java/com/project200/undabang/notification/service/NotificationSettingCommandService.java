package com.project200.undabang.notification.service;

import com.project200.undabang.notification.dto.request.UpdateDeviceNotificationSettingRequest;
import com.project200.undabang.notification.dto.response.UpdateDeviceNotificationSettingResponse;

import java.util.List;

public interface NotificationSettingCommandService {

    UpdateDeviceNotificationSettingResponse updateDeviceNotificationSetting(String fcmToken, List<UpdateDeviceNotificationSettingRequest> requestList);
}
