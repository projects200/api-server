package com.project200.undabang.timer.custom.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetCustomTimerListResponse {
    private int customTimerCount;
    private List<CustomTimerRecord> customTimers;
}

