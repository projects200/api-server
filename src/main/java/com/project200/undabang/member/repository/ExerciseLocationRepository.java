package com.project200.undabang.member.repository;

import com.project200.undabang.member.entity.ExerciseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseLocationRepository extends JpaRepository<ExerciseLocation, Long>, ExerciseLocationRepositoryCustom {
}
