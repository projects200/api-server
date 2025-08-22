package com.project200.undabang.timer.custom.repository;

import com.project200.undabang.timer.custom.entity.CustomTimer;

public interface CustomTimerStepRepositoryCustom {
    void softDeleteAllByCustomTimer(CustomTimer customTimer);
}
