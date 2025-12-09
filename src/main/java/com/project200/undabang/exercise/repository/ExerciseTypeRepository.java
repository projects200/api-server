package com.project200.undabang.exercise.repository;

import com.project200.undabang.exercise.entity.ExerciseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseTypeRepository extends JpaRepository<ExerciseType, Long> {
    List<ExerciseType> findAllByExerciseTypeDeletedAtNullOrderBySelectionCountDesc();
}
