package com.project200.undabang.exercise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindExerciseRecordDateResponseDto {
    private Long exerciseId;
    private String exerciseTitle;
    private String exercisePersonalType;
    private LocalDateTime exerciseStartedAt;
    private LocalDateTime exerciseEndedAt;
    private String pictureUrl;
}
