package com.project200.undabang.timer.simple.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SimpleTimerUpdateRequestDto {
    @Min(value = 1, message = "시간은 항상 양수여야 합니다!")
    @Max(value = 3599, message = "시간은 59분 59초를 초과할 수 없습니다!")
    private int time;
}
