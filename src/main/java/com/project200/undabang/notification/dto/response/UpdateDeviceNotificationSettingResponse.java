package com.project200.undabang.notification.dto.response;

import com.project200.undabang.notification.dto.record.NotificationSettingRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeviceNotificationSettingResponse {
    private String fcmToken;
    private List<NotificationSettingRecord> settings;

    public static UpdateDeviceNotificationSettingResponse of(String fcmToken, List<NotificationSettingRecord> settings) {
        return UpdateDeviceNotificationSettingResponse.builder()
                .fcmToken(fcmToken)
                .settings(settings)
                .build();
    }
}
