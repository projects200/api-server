package com.project200.undabang.timer.simple.service;

import com.project200.undabang.member.dto.event.MemberSignedUpEvent;

public interface SimpleTimerCommandService {
    void createDefaultSimpleTimer(MemberSignedUpEvent event);
}
