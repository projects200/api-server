package com.project200.undabang.timer.custom.dto.response;

public record CustomTimerRecord(Long customTimerId, String customTimerName) {
    public static CustomTimerRecord of(Long customTimerId, String customTimerName) {
        return new CustomTimerRecord(customTimerId, customTimerName);
    }
}
