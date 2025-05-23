package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.service.CreateExerciseService;
import com.project200.undabang.exercise.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ExerciseServiceImpl implements ExerciseService {

    private final CreateExerciseService createExerciseService;

    @Override
    public CreateExerciseResponseDto uploadExerciseImages(CreateExerciseRequestDto requestDto) throws CustomException {
        return createExerciseService.uploadExerciseImages(requestDto);
    }
}
