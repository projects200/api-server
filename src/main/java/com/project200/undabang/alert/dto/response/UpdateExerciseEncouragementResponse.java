package com.project200.undabang.alert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExerciseEncouragementResponse {
    private long updatedTokenCount;

    public static UpdateExerciseEncouragementResponse of(long updatedFcmTokenCount) {
        return new UpdateExerciseEncouragementResponse(updatedFcmTokenCount);
    }
}
