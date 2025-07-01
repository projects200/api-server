package com.project200.undabang.exercise.repository;

import com.project200.undabang.exercise.entity.ExercisePicture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExercisePictureRepository extends JpaRepository<ExercisePicture, Long>, ExercisePictureRepositoryCustom {

    List<ExercisePicture> findAllByExercise_Id(Long exerciseId);
}
