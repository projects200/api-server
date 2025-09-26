package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.request.CreateExerciseLocationRequest;
import com.project200.undabang.member.dto.response.CreateExerciseLocationResponse;

public interface ExerciseLocationCommandService {
    CreateExerciseLocationResponse createExerciseLocation(CreateExerciseLocationRequest request);
}
