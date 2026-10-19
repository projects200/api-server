package com.project200.undabang.timer.simple.dto.response;

import com.project200.undabang.timer.simple.entity.SimpleTimer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTimerCreateResponseDto {
    private Long simpleTimerId;
    private LocalDateTime updatedAt;

    public static SimpleTimerCreateResponseDto from(SimpleTimer simpleTimer) {
        return SimpleTimerCreateResponseDto.builder()
                .simpleTimerId(simpleTimer.getId())
                .updatedAt(simpleTimer.getSimpleTimerUpdatedAt())
                .build();
    }
}
