package com.project200.undabang.notification.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.notification.dto.response.GetAllDeviceNotificationSettingsResponse;
import com.project200.undabang.notification.service.NotificationSettingQueryService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class NotificationSettingQueryController {

    private final NotificationSettingQueryService notificationSettingQueryService;

    @GetMapping("/v1/notification-settings/device")
    public ResponseEntity<CommonResponse<List<GetAllDeviceNotificationSettingsResponse>>> getAllDeviceNotificationSettings(@RequestHeader("X-Fcm-Token") @NotBlank(message = "FCM TOKEN 값은 공백일 수 없습니다.") String fcmToken) {

        return ResponseEntity.ok(CommonResponse.success(notificationSettingQueryService.getAllDeviceNotificationSettings(fcmToken)));
    }
}
