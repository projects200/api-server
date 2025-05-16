package com.project200.undabang.exercise.repository;

import com.project200.undabang.exercise.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long>, com.project200.undabang.exercise.repository.querydsl.ExerciseRepositoryCustom {

}
