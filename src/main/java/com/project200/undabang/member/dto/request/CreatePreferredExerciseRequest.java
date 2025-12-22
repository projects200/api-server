package com.project200.undabang.member.dto.request;

import com.project200.undabang.member.enums.ExerciseSkillLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreatePreferredExerciseRequest {
    @NotNull
    private Long exerciseTypeId;

    @NotNull
    private ExerciseSkillLevel skillLevel;

    @NotNull
    private boolean[] daysOfWeek;
}
