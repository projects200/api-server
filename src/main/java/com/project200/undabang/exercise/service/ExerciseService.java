package com.project200.undabang.exercise.service;

import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;

import java.io.IOException;

public interface ExerciseService {

    CreateExerciseResponseDto uploadExerciseImages(CreateExerciseRequestDto requestDto) throws IOException;
    CreateExerciseResponseDto updateExerciseImages(Long exerciseId, UpdateExerciseRequestDto requestDto) throws IOException;
}
