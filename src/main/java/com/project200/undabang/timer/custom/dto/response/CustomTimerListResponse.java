package com.project200.undabang.timer.custom.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomTimerListResponse {
    private int customTimerCount;
    private List<CustomTimerRecord> customTimers;

    /**
     * 주어진 커스텀 타이머 레코드 목록을 기반으로 GetCustomTimerListResponse 객체를 생성합니다.
     */
    public static CustomTimerListResponse from(List<CustomTimerRecord> customTimers) {
        return CustomTimerListResponse.builder()
                .customTimerCount(customTimers.size())
                .customTimers(customTimers)
                .build();
    }
}