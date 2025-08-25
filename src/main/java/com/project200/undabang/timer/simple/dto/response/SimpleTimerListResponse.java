package com.project200.undabang.timer.simple.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTimerListResponse {
    private int simpleTimerCount;
    private List<SimpleTimerRecord> simpleTimers;

    public static SimpleTimerListResponse of(List<SimpleTimerRecord> simpleTimers) {
        return SimpleTimerListResponse.builder()
                .simpleTimerCount(simpleTimers.size())
                .simpleTimers(simpleTimers)
                .build();
    }
}
