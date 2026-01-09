package com.project200.undabang.member.dto.record;

import com.project200.undabang.member.enums.ExerciseSkillLevel;

public record PreferredExerciseRecord(
        Long preferredExerciseId,
        String name,
        ExerciseSkillLevel skillLevel,
        Byte daysOfWeek,
        String imageUrl) {

    public boolean[] getDaysOfWeek() {
        boolean[] days = new boolean[7];
        byte dateValue = (daysOfWeek != null) ? daysOfWeek : 0;

        for (int i = 0; i < 7; i++) {
            days[i] = (dateValue & (1 << i)) != 0;
        }

        return days;
    }
}
