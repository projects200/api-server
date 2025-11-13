package com.project200.undabang.notification.dto.request;

import com.project200.undabang.notification.fcm.entity.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeviceNotificationSettingRequest {
    @NotNull(message = "알림 타입을 입력해주세요.")
    private NotificationType type;

    @NotNull(message = "활성화 여부를 입력해주세요.")
    private Boolean enabled;
}
