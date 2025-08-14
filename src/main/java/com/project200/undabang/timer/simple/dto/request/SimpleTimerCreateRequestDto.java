package com.project200.undabang.timer.simple.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SimpleTimerCreateRequestDto {
    @Min(value = 1, message = "0보다 커야 합니다.")
    @Max(value = 3599, message = "1시간 보다 작아야 합니다.")
    private Integer time;
}
