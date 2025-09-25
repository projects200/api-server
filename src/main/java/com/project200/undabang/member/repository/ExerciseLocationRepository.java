package com.project200.undabang.member.repository;

import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseLocationRepository extends JpaRepository<ExerciseLocation, Long>, ExerciseLocationRepositoryCustom {
    List<ExerciseLocation> findAllByMemberAndExerciseLocationDeletedAtNull(Member member);

    boolean existsByMemberAndExerciseLocationNameAndExerciseLocationDeletedAtNull(Member member, String exerciseLocationName);
    long countByMemberAndExerciseLocationDeletedAtNull(Member member);
}
