package com.project200.undabang.member.dto.response;

import lombok.Data;

@Data
public class PreferredExercisesOfMemberResponse {
    private Long preferredExerciseId;
    private String name;
    private String skillLevel;
    private boolean[] daysOfWeek;
    private String imageUrl;
}
