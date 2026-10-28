package com.project200.undabang.exercise.sync.repository;

import com.project200.undabang.exercise.sync.entity.ExerciseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExerciseReceiptRepository extends JpaRepository<ExerciseReceipt, Long> {
    Optional<ExerciseReceipt> findByMember_MemberIdAndRequestId(UUID memberId, UUID requestId);
}
