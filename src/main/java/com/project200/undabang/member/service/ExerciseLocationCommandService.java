package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.request.CreateExerciseLocationRequest;
import com.project200.undabang.member.dto.request.UpdateExerciseLocationRequest;
import com.project200.undabang.member.dto.response.CreateExerciseLocationResponse;
import com.project200.undabang.member.dto.response.UpdateExerciseLocationResponse;

public interface ExerciseLocationCommandService {
    CreateExerciseLocationResponse createExerciseLocation(CreateExerciseLocationRequest request);
    UpdateExerciseLocationResponse updateExerciseLocation(Long locationId, UpdateExerciseLocationRequest request);

    void deleteExerciseLocation(Long locationId);
}
