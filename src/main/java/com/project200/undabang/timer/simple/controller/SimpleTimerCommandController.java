package com.project200.undabang.timer.simple.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SimpleTimerCommandController {
    private final SimpleTimerCommandService simpleTimerCommandService;

    @PatchMapping("/v1/simple-timers/{simpleTimerId}")
    public ResponseEntity<CommonResponse<Void>> updateSimpleTimer(@PathVariable("simpleTimerId") Long simpleTimerId,
                                                                  @Valid @RequestBody SimpleTimerUpdateRequestDto dto) {

        simpleTimerCommandService.updateSimpleTimer(simpleTimerId, dto);
        return ResponseEntity.ok(CommonResponse.update(null));
    }
}
