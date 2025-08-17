package com.project200.undabang.timer.simple.dto.response;

import com.project200.undabang.timer.simple.entity.SimpleTimer;
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

    public static SimpleTimerCreateResponseDto from(SimpleTimer simpleTimer) {
        return SimpleTimerCreateResponseDto.builder()
                .simpleTimerId(simpleTimer.getId())
                .build();
    }
}
