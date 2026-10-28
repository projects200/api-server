package com.project200.undabang.exercise.service;

import com.project200.undabang.exercise.dto.response.ExercisePicturesResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ExercisePictureService {

    ExercisePicturesResponse uploadExercisePictures(Long exerciseId, List<MultipartFile> exercisePictureList,
                                                    UUID uploadRequestId, List<UUID> clientImageIds);
    boolean deleteExercisePictures(UUID memberId, Long exerciseId, List<Long> pictureIds);
    List<Long> getAllImagesFromExercise(UUID memberId, Long exerciseId);
}