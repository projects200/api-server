package com.project200.undabang.timer.custom.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.custom.service.CustomTimerCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomTimerCommandController {
    private final CustomTimerCommandService customTimerCommandService;

    @PostMapping("/v1/custom-timers")
    public ResponseEntity<CommonResponse<CustomTimerCreateResponse>> createCustomTimer(@Valid @RequestBody CustomTimerCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(customTimerCommandService.createCustomTimer(request)));
    }

    @DeleteMapping("/v1/custom-timers/{customTimerId}")
    public ResponseEntity<CommonResponse<Void>> deleteCustomTimer(@PathVariable Long customTimerId) {

        customTimerCommandService.deleteCustomTimer(customTimerId);
        return ResponseEntity.ok(CommonResponse.delete(null));
    }

}
