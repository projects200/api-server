package com.project200.undabang.exercise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindExerciseRecordByPeriodResponseDto {
    private LocalDate date;
    private Long exerciseCount;
}
