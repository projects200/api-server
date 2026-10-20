package com.project200.undabang.timer.simple.dto.response;

import com.project200.undabang.timer.simple.entity.SimpleTimer;

import java.time.LocalDateTime;

public record SimpleTimerRecord(Long simpleTimerId, int time, LocalDateTime updatedAt) {

    public static SimpleTimerRecord from(SimpleTimer simpleTimer) {
        return new SimpleTimerRecord(simpleTimer.getId(), simpleTimer.getSimpleTimerTime(),
                simpleTimer.getSimpleTimerUpdatedAt());
    }
}
