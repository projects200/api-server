package com.project200.undabang.timer.custom.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerNameUpdateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerUpdateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.custom.service.CustomTimerCommandService;
import com.project200.undabang.timer.sync.TimerDeleteRequest;
import com.project200.undabang.timer.sync.TimerMutationResponse;
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
    public ResponseEntity<CommonResponse<TimerMutationResponse>> deleteCustomTimer(@PathVariable Long customTimerId,
                                                                                 @RequestBody(required = false) TimerDeleteRequest request) {
        return ResponseEntity.ok(CommonResponse.delete(customTimerCommandService.deleteCustomTimer(customTimerId, request)));
    }

    @PatchMapping("/v1/custom-timers/{customTimerId}")
    public ResponseEntity<CommonResponse<TimerMutationResponse>> updateCustomTimerName(@PathVariable("customTimerId") Long customTimerId,
                                                                                     @Valid @RequestBody CustomTimerNameUpdateRequest request) {
        return ResponseEntity.ok(CommonResponse.update(customTimerCommandService.updateCustomTimerName(customTimerId, request)));
    }

    @PutMapping("/v1/custom-timers/{customTimerId}")
    public ResponseEntity<CommonResponse<TimerMutationResponse>> updateCustomTimer(@PathVariable("customTimerId") Long customTimerId,
                                                                                 @Valid @RequestBody CustomTimerUpdateRequest request) {
        return ResponseEntity.ok(CommonResponse.update(customTimerCommandService.updateCustomTimer(customTimerId, request)));
    }
}
