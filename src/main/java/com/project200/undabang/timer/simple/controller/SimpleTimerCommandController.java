package com.project200.undabang.timer.simple.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerCreateRequestDto;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;
import com.project200.undabang.timer.simple.dto.response.SimpleTimerCreateResponseDto;
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SimpleTimerCommandController {
    private final SimpleTimerCommandService simpleTimerCommandService;

    @PostMapping("/v1/simple-timers")
    public ResponseEntity<CommonResponse<SimpleTimerCreateResponseDto>> createSimpleTimer(@Valid @RequestBody SimpleTimerCreateRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(simpleTimerCommandService.createSimpleTimer(dto)));
    }

    @PatchMapping("/v1/simple-timers/{simpleTimerId}")
    public ResponseEntity<CommonResponse<Void>> updateSimpleTimer(@PathVariable("simpleTimerId") Long simpleTimerId,
                                                                  @Valid @RequestBody SimpleTimerUpdateRequestDto dto) {

        simpleTimerCommandService.updateSimpleTimer(simpleTimerId, dto);
        return ResponseEntity.ok(CommonResponse.update(null));
    }

    @DeleteMapping("/v1/simple-timers/{simpleTimerId}")
    public ResponseEntity<CommonResponse<Void>> deleteSimpleTimer(@PathVariable("simpleTimerId") Long simpleTimerId) {
        simpleTimerCommandService.deleteSimpleTimer(simpleTimerId);
        return ResponseEntity.ok(CommonResponse.delete(null));
    }
}
