package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.entity.ExerciseLocation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExerciseLocationResponse {
    private long id;

    public static UpdateExerciseLocationResponse from(ExerciseLocation exerciseLocation) {
        return new UpdateExerciseLocationResponse(exerciseLocation.getExerciseLocationId());
    }
}
