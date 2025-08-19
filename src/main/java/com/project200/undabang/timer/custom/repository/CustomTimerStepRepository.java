package com.project200.undabang.timer.custom.repository;

import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomTimerStepRepository extends JpaRepository<CustomTimerStep, Long> {
}
