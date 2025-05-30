package com.project200.undabang.exercise.service;

import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExerciseCommandService {

    ExerciseIdResponseDto createExercise(CreateExerciseRequestDto requestDto);

    ExerciseIdResponseDto uploadExerciseImages(Long exerciseId, List<MultipartFile> exercisePictureList);

    ExerciseIdResponseDto updateExercise(Long exerciseId, UpdateExerciseRequestDto requestDto);

    void deleteExercise(Long exerciseId);

    void deleteImages(Long exerciseId, List<Long> pictureIds);
}
