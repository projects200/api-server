package com.project200.undabang.exercise.sync.repository;

import com.project200.undabang.exercise.sync.entity.ExercisePhotoCleanup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExercisePhotoCleanupRepository extends JpaRepository<ExercisePhotoCleanup, Long> {
    List<ExercisePhotoCleanup> findTop50ByNextAttemptAtLessThanEqualOrderById(LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from ExercisePhotoCleanup task where task.id = :id")
    Optional<ExercisePhotoCleanup> findByIdForUpdate(Long id);
}
