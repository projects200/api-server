package com.project200.undabang.timer.simple.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTimerCreateResponseDto {
    private Long simpleTimerId;
}
