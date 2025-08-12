package com.project200.undabang.timer.simple.dto;

import com.project200.undabang.timer.simple.entity.SimpleTimer;

public record SimpleTimerRecord(Long simpleTimerId, int time) {

    public static SimpleTimerRecord from(SimpleTimer simpleTimer) {
        return new SimpleTimerRecord(simpleTimer.getId(), simpleTimer.getSimpleTimerTime());
    }
}
