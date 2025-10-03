package com.project200.undabang.alert.controller;

import com.project200.undabang.alert.service.AlertService;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.common.web.response.SuccessDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlertController {

    private final AlertService alertService;

    /**
     * 사용자 에이전트 및 FCM 토큰 정보를 기반으로 알림 기능을 활성화합니다.
     */
    @PatchMapping("/v1/alerts/activate")
    public ResponseEntity<CommonResponse<Void>> activateAlert(@RequestHeader(value = "X-Fcm-Token") String fcmToken) {

        alertService.activateAlert(fcmToken);

        return ResponseEntity.ok(CommonResponse.success(new SuccessDetails("ACTIVATE_ALERTS", "알림 기능이 활성화 되었습니다.")));
    }

    /**
     * 주어진 FCM 토큰을 기반으로 알림 기능을 비활성화합니다.
     */
    @PatchMapping("/v1/alerts/deactivate")
    public ResponseEntity<CommonResponse<Void>> deactivateAlert(@RequestHeader(value = "X-Fcm-Token") String fcmToken) {

        alertService.deactivateAlert(fcmToken);

        return ResponseEntity.ok(CommonResponse.success(new SuccessDetails("DEACTIVATE_ALERTS", "알림 기능이 비활성화 되었습니다.")));
    }
}
