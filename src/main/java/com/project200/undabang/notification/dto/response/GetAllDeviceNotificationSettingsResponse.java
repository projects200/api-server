package com.project200.undabang.notification.dto.response;

import com.project200.undabang.notification.fcm.entity.DeviceNotificationSetting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAllDeviceNotificationSettingsResponse {
    private String type;
    private boolean enabled;

    /**
     * 주어진 디바이스 알림 설정 객체를 기반으로 GetAllDeviceNotificationSettingsResponse 객체를 생성합니다.
     */
    public static GetAllDeviceNotificationSettingsResponse from(DeviceNotificationSetting setting) {
        return GetAllDeviceNotificationSettingsResponse.builder()
                .type(setting.getNotificationType().getNotificationTypeCode())
                .enabled(setting.getIsEnabled())
                .build();
    }
}
