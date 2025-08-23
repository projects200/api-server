package com.project200.undabang.timer.custom.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.custom.service.CustomTimerCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomTimerCommandController {
    private final CustomTimerCommandService customTimerCommandService;

    @PostMapping("/v1/custom-timers")
    public ResponseEntity<CommonResponse<CustomTimerCreateResponse>> createCustomTimer(@RequestBody CustomTimerCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(customTimerCommandService.createCustomTimer(request)));
    }

}
