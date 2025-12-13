package com.project200.undabang.member.dto.response;

import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 보유한 선호 운동 응답 DTO입니다.
 * 운동 종류, 운동 주기, 운동 수준을 포함합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPreferredExerciseResponse {
    private Long preferredExerciseId;
    private Long exerciseTypeId;
    private String exerciseName;
    private ExerciseSkillLevel skillLevel;
    private boolean[] daysOfWeek;
    private String imageUrl;
    
    public static MyPreferredExerciseResponse from(PreferredExercise preferredExercise) {
        return MyPreferredExerciseResponse.builder()
                .preferredExerciseId(preferredExercise.getId())
                .exerciseTypeId(preferredExercise.getExercise().getId())
                .exerciseName(preferredExercise.getExercise().getExerciseName())
                .skillLevel(preferredExercise.getPreferredExerciseSkillLevel())
                .daysOfWeek(preferredExercise.getDaysOfWeek())
                .imageUrl(preferredExercise.getExercise().getExerciseTypeImageUrl())
                .build();
    }
}


