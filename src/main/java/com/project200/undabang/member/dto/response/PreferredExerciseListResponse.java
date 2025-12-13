package com.project200.undabang.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferredExerciseListResponse {
    private List<MyPreferredExerciseResponse> preferredExercises;

    public static PreferredExerciseListResponse from(List<MyPreferredExerciseResponse> preferredExercises) {
        return PreferredExerciseListResponse.builder()
                .preferredExercises(preferredExercises)
                .build();
    }
}
