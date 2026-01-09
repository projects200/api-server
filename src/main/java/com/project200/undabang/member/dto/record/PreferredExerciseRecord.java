package com.project200.undabang.member.dto.record;

import com.project200.undabang.member.enums.ExerciseSkillLevel;

public record PreferredExerciseRecord(
        String exerciseName,
        Byte preferredExerciseDate,
        ExerciseSkillLevel skillLevel) {
}
