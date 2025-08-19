package com.project200.undabang.timer.custom.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomTimerCreateRequest {
    private String customTimerName;
    private List<CustomTimerStepCreateRequest> customTimerSteps;
}
