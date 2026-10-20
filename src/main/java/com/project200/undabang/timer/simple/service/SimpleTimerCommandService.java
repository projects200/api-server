package com.project200.undabang.timer.simple.service;

import com.project200.undabang.timer.simple.dto.request.SimpleTimerCreateRequestDto;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;
import com.project200.undabang.timer.simple.dto.response.SimpleTimerCreateResponseDto;
import com.project200.undabang.timer.sync.TimerDeleteRequest;
import com.project200.undabang.timer.sync.TimerMutationResponse;

import java.util.UUID;

public interface SimpleTimerCommandService {
    SimpleTimerCreateResponseDto createSimpleTimer(SimpleTimerCreateRequestDto dto);
    void createDefaultSimpleTimer(UUID memberId);
    TimerMutationResponse updateSimpleTimer(Long simpleTimerId, SimpleTimerUpdateRequestDto dto);
    TimerMutationResponse deleteSimpleTimer(Long simpleTimerId, TimerDeleteRequest dto);
}
