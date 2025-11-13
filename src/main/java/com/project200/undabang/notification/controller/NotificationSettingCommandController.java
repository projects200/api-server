package com.project200.undabang.notification.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.notification.dto.request.UpdateDeviceNotificationSettingRequest;
import com.project200.undabang.notification.dto.response.UpdateDeviceNotificationSettingResponse;
import com.project200.undabang.notification.service.NotificationSettingCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class NotificationSettingCommandController {

    private final NotificationSettingCommandService notificationSettingCommandService;

    @PatchMapping("/v1/notification-settings/device")
    public ResponseEntity<CommonResponse<UpdateDeviceNotificationSettingResponse>> updateDeviceNotificationSettings(@RequestHeader(value = "X-Fcm-Token") @NotBlank(message = "FCM TOKEN 값은 공백일 수 없습니다.") String fcmToken,
                                                                                                                    @RequestBody @NotEmpty(message = "알림 타입을 입력해주세요") @Valid List<UpdateDeviceNotificationSettingRequest> requestList) {


        return ResponseEntity.ok(CommonResponse.success(notificationSettingCommandService.updateDeviceNotificationSetting(fcmToken, requestList)));
    }
}
