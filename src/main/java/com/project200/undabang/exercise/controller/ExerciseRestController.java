package com.project200.undabang.exercise.controller;

import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.service.ExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/exercises")
public class ExerciseRestController {

    private final ExerciseService exerciseService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CreateExerciseResponseDto createExercise(@Valid @ModelAttribute CreateExerciseRequestDto requestDto) throws IOException {
        CreateExerciseResponseDto createExerciseResponseDto = exerciseService.uploadExerciseImages(requestDto);
        return createExerciseResponseDto;
    }
}
