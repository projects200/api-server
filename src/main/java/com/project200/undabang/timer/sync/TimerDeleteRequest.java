package com.project200.undabang.timer.sync;

import java.util.UUID;

public record TimerDeleteRequest(UUID editId, Long editedAt) {
}
