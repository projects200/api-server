package com.project200.undabang.score.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record EarnablePointsInfoResponseDto(
        int pointsPerExercise,
        byte currentUserScore,
        int maxScore,
        ValidityWindowDto validWindow,
        @JsonFormat(pattern = "yyyy-MM-dd")
        List<LocalDate> earnableScoreDates
) {
}