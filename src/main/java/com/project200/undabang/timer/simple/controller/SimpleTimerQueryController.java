package com.project200.undabang.timer.simple.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.timer.simple.dto.response.GetSimpleTimerResponseDto;
import com.project200.undabang.timer.simple.service.SimpleTimerQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SimpleTimerQueryController {
    private final SimpleTimerQueryService simpleTimerQueryService;

    @GetMapping("/v1/simple-timers")
    public ResponseEntity<CommonResponse<GetSimpleTimerResponseDto>> getSimpleTimers() {
        return ResponseEntity.ok(CommonResponse.success(simpleTimerQueryService.getSimpleTimers()));
    }
}
