package com.project200.undabang.policy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExercisePolicyResponseDto {
    private String minPoint;
    private String maxPoint;
    private String initialPoint;
    private String pointPerExercise;
    private String penaltyPoint;
    private String validityPeriod;
    private String penaltyThresholdDay;
}
