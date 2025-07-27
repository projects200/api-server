package com.project200.undabang.score.dto.response;

import java.time.LocalDate;
import java.util.List;

public record EarnablePointsInfoResponseDto(
        byte pointsPerExercise,
        byte currentUserScore,
        byte maxScore,
        ValidityWindowDto validWindow,
        List<LocalDate> earnableScoreDates
) {
}