package com.project200.undabang.timer.custom.dto.response;

import java.time.LocalDateTime;

public record CustomTimerRecord(Long customTimerId, String customTimerName, LocalDateTime updatedAt) {
}