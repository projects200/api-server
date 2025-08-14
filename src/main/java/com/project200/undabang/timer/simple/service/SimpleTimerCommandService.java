package com.project200.undabang.timer.simple.service;

import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;

import java.util.UUID;

public interface SimpleTimerCommandService {
    void createDefaultSimpleTimer(UUID memberId);

    void updateSimpleTimer(Long simpleTimerId, SimpleTimerUpdateRequestDto dto);
}
