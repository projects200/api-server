package com.project200.undabang.member.repository;

import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.PreferredExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreferredExerciseRepository extends JpaRepository<PreferredExercise, Long> {
    List<PreferredExercise> findAllByMemberAndPreferredExerciseDeletedAtNull(Member member);
}


