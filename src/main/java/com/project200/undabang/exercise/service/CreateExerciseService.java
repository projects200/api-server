package com.project200.undabang.exercise.service;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;

public interface CreateExerciseService {

    CreateExerciseResponseDto uploadExerciseImages(CreateExerciseRequestDto requestDto) throws CustomException;

}
