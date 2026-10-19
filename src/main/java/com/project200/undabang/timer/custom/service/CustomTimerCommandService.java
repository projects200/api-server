package com.project200.undabang.timer.custom.service;

import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerNameUpdateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerUpdateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.sync.TimerDeleteRequest;
import com.project200.undabang.timer.sync.TimerMutationResponse;

public interface CustomTimerCommandService {
    CustomTimerCreateResponse createCustomTimer(CustomTimerCreateRequest dto);
    TimerMutationResponse deleteCustomTimer(Long customTimerId, TimerDeleteRequest request);
    TimerMutationResponse updateCustomTimerName(Long customTimerId, CustomTimerNameUpdateRequest request);
    TimerMutationResponse updateCustomTimer(Long customTimerId, CustomTimerUpdateRequest request);
}
