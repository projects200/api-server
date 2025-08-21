package com.project200.undabang.timer.custom.dto.response;

import com.project200.undabang.timer.custom.entity.CustomTimer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomTimerDetailResponse {
    List<CustomTimerStepRecord> customTimerSteps;
    private long customTimerId;
    private String customTimerName;
    private int customTimerStepCount;

    public static CustomTimerDetailResponse from(CustomTimer customTimer, List<CustomTimerStepRecord> customTimerSteps) {
        return CustomTimerDetailResponse.builder()
                .customTimerId(customTimer.getId())
                .customTimerName(customTimer.getCustomTimerName())
                .customTimerStepCount(customTimerSteps.size())
                .customTimerSteps(customTimerSteps)
                .build();
    }
}
