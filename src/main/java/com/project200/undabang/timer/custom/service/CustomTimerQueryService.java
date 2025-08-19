package com.project200.undabang.timer.custom.service;

import com.project200.undabang.timer.custom.dto.response.CustomTimerDetailResponse;
import com.project200.undabang.timer.custom.dto.response.CustomTimerListResponse;

public interface CustomTimerQueryService {
    CustomTimerListResponse getCustomTimerList();

    CustomTimerDetailResponse getCustomTimerDetail(Long customTimerId);
}
