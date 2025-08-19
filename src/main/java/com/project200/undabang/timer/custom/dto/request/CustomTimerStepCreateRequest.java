package com.project200.undabang.timer.custom.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomTimerStepCreateRequest {
    private String customTimerStepName;
    private byte customTimerStepOrder;
    private int customTimerStepTime;
}
