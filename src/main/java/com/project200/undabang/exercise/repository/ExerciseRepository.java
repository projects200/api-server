package com.project200.undabang.exercise.repository;

import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long>, ExerciseRepositoryCustom {
    Long member(Member member);
}
