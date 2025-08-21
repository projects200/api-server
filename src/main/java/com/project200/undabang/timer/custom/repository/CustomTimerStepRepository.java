package com.project200.undabang.timer.custom.repository;

import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomTimerStepRepository extends JpaRepository<CustomTimerStep, Long> {
    List<CustomTimerStep> findAllByCustomTimerAndCustomTimerStepDeletedAtNull(CustomTimer customTimer);
}
