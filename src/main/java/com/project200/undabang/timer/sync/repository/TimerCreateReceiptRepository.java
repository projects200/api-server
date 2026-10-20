package com.project200.undabang.timer.sync.repository;

import com.project200.undabang.timer.sync.entity.TimerCreateReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TimerCreateReceiptRepository extends JpaRepository<TimerCreateReceipt, Long> {
    Optional<TimerCreateReceipt> findByMember_MemberIdAndCreateRequestId(UUID memberId, UUID createRequestId);
}
