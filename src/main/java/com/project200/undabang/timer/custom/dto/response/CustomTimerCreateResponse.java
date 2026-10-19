package com.project200.undabang.timer.custom.dto.response;

import java.time.LocalDateTime;

public record CustomTimerCreateResponse(Long customTimerId, LocalDateTime updatedAt) {
}
