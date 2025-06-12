package com.project200.undabang.exercise.repository;

public interface ExercisePictureRepositoryCustom {
    long countNotDeletedPicturesByExerciseId(Long exerciseId);
}
