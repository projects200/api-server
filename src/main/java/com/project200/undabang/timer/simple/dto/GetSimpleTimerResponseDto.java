package com.project200.undabang.timer.simple.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetSimpleTimerResponseDto {
    private int simpleTimerCount;
    private List<SimpleTimerRecord> simpleTimers;

    public static GetSimpleTimerResponseDto of(List<SimpleTimerRecord> simpleTimers){
        return GetSimpleTimerResponseDto.builder()
                .simpleTimerCount(simpleTimers.size())
                .simpleTimers(simpleTimers)
                .build();
    }
}
