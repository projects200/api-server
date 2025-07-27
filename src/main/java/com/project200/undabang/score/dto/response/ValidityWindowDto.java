package com.project200.undabang.score.dto.response;

import java.time.LocalDateTime;

public record ValidityWindowDto(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {
}