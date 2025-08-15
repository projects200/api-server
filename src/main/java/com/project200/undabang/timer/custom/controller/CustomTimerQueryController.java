package com.project200.undabang.timer.custom.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.timer.custom.dto.response.GetCustomTimerListResponse;
import com.project200.undabang.timer.custom.service.CustomTimerQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomTimerQueryController {
    private final CustomTimerQueryService customTimerQueryService;

    @GetMapping("/v1/custom-timers")
    public ResponseEntity<CommonResponse<GetCustomTimerListResponse>> getCustomTimerList() {

        return ResponseEntity.ok(CommonResponse.success(customTimerQueryService.getCustomTimerList()));
    }
}
