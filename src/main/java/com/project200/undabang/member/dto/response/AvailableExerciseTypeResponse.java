package com.project200.undabang.member.dto.response;

import com.project200.undabang.exercise.entity.ExerciseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 선택 가능한 운동 종류 응답 DTO입니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableExerciseTypeResponse {
    private Long exerciseId;
    private String exerciseName;
    private String imageUrl;
    
    public static AvailableExerciseTypeResponse from(ExerciseType exerciseType) {
        return AvailableExerciseTypeResponse.builder()
                .exerciseId(exerciseType.getId())
                .exerciseName(exerciseType.getExerciseName())
                .imageUrl(exerciseType.getExerciseTypeImageUrl())
                .build();
    }
}


