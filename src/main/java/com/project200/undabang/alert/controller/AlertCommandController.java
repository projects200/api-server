package com.project200.undabang.alert.controller;

import com.project200.undabang.alert.dto.response.UpdateExerciseEncouragementResponse;
import com.project200.undabang.alert.service.AlertCommandService;
import com.project200.undabang.common.web.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlertCommandController {

    private final AlertCommandService alertCommandService;

    /**
     * 사용자 에이전트 및 FCM 토큰 정보를 기반으로 알림 기능을 활성화합니다.
     */
    @PatchMapping("/v1/alerts/exercise-encouragement/activate")
    public ResponseEntity<CommonResponse<UpdateExerciseEncouragementResponse>> activateAlert() {

        return ResponseEntity.ok(CommonResponse.success(alertCommandService.activateAllExerciseEncouragementToken()));
    }

    /**
     * 주어진 FCM 토큰을 기반으로 알림 기능을 비활성화합니다.
     */
    @PatchMapping("/v1/alerts/exercise-encouragement/deactivate")
    public ResponseEntity<CommonResponse<UpdateExerciseEncouragementResponse>> deactivateAlert() {

        return ResponseEntity.ok(CommonResponse.success(alertCommandService.deactivateAllExerciseEncouragementToken()));
    }
}
