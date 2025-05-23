package com.project200.undabang.exercise.repository;

import com.project200.undabang.exercise.entity.ExercisePicture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExercisePictureRepository extends JpaRepository<ExercisePicture, Long> {
    List<ExercisePicture> findByExercise_Id(Long exerciseId);
}
