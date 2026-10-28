package com.project200.undabang.exercise.service;

import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.dto.response.ExercisePicturesResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ExerciseCommandService {

    CreateExerciseResponseDto createExercise(CreateExerciseRequestDto requestDto);

    ExercisePicturesResponse uploadExerciseImages(Long exerciseId, List<MultipartFile> exercisePictureList,
                                                  UUID uploadRequestId, List<UUID> clientImageIds);

    ExerciseIdResponseDto updateExercise(Long exerciseId, UpdateExerciseRequestDto requestDto);

    ExerciseIdResponseDto deleteExercise(Long exerciseId, UUID editId);

    ExerciseIdResponseDto deleteImages(Long exerciseId, List<Long> pictureIds, UUID editId);
}
