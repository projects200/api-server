package com.project200.undabang.timer.sync.repository;

import com.project200.undabang.timer.sync.TimerType;
import com.project200.undabang.timer.sync.entity.TimerEditReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TimerEditReceiptRepository extends JpaRepository<TimerEditReceipt, Long> {
    Optional<TimerEditReceipt> findByMember_MemberIdAndTimerTypeAndTimerIdAndEditId(
            UUID memberId, TimerType timerType, Long timerId, UUID editId);
}
