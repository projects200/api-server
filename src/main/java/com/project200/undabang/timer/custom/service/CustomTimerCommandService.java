package com.project200.undabang.timer.custom.service;

import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerNameUpdateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;

public interface CustomTimerCommandService {
    CustomTimerCreateResponse createCustomTimer(CustomTimerCreateRequest dto);
    void deleteCustomTimer(Long customTimerId);
    void updateCustomTimerName(Long customTimerId, CustomTimerNameUpdateRequest request);

    void updateCustomTimer(Long customTimerId, CustomTimerCreateRequest request);
}
