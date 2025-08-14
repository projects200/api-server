package com.project200.undabang.timer.simple.service;

import com.project200.undabang.timer.simple.dto.request.SimpleTimerCreateRequestDto;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;
import com.project200.undabang.timer.simple.dto.response.SimpleTimerCreateResponseDto;

import java.util.UUID;

public interface SimpleTimerCommandService {
    SimpleTimerCreateResponseDto createSimpleTimer(SimpleTimerCreateRequestDto dto);
    void createDefaultSimpleTimer(UUID memberId);
    void updateSimpleTimer(Long simpleTimerId, SimpleTimerUpdateRequestDto dto);
    void deleteSimpleTimer(Long simpleTimerId);
}
