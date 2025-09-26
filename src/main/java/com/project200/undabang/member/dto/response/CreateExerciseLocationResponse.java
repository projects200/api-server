package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.entity.ExerciseLocation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateExerciseLocationResponse {
    private Long exerciseLocationId;

    public static CreateExerciseLocationResponse from(ExerciseLocation exerciseLocation) {
        return new CreateExerciseLocationResponse(exerciseLocation.getExerciseLocationId());
    }
}
