package com.project200.undabang.timer.sync;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import lombok.Getter;

@Getter
public class TimerSyncConflictException extends CustomException {
    private final Object currentValue;

    public TimerSyncConflictException(Object currentValue) {
        super(ErrorCode.TIMER_EDIT_CONFLICT);
        this.currentValue = currentValue;
    }
}
