package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferredExercisesOfMemberResponse {
    private Long preferredExerciseId;
    private String name;
    private ExerciseSkillLevel skillLevel;
    private boolean[] daysOfWeek;
    private String imageUrl;

    public PreferredExercisesOfMemberResponse(PreferredExercise preferredExercise) {
        this.preferredExerciseId = preferredExercise.getId();
        this.name = preferredExercise.getExercise().getExerciseName();
        this.skillLevel = preferredExercise.getPreferredExerciseSkillLevel();
        this.daysOfWeek = preferredExercise.getDaysOfWeek();
        this.imageUrl = preferredExercise.getExercise().getExerciseTypeImageUrl();
    }
}
