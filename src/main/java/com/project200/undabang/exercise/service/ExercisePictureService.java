package com.project200.undabang.exercise.service;

import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExercisePictureService {

    ExerciseIdResponseDto uploadExercisePictures(Long exerciseId, List<MultipartFile> exercisePictureList);

}
