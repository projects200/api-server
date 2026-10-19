package com.project200.undabang.timer.sync;

import java.time.LocalDateTime;

public record TimerCreateResult(Long timerId, LocalDateTime updatedAt) {
}
