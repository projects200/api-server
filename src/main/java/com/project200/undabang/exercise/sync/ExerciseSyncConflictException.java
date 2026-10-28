package com.project200.undabang.exercise.sync;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import lombok.Getter;

@Getter
public class ExerciseSyncConflictException extends CustomException {
    private final ExerciseIdResponseDto currentValue;

    public ExerciseSyncConflictException(Long exerciseId, long version) {
        super(ErrorCode.EXERCISE_EDIT_CONFLICT);
        currentValue = new ExerciseIdResponseDto(exerciseId, version);
    }
}
