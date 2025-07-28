package com.project200.undabang.score.dto.internal;

import lombok.Getter;

/**
 * 운동 기록에 대한 점수 획득 가능 여부와 획득 점수를 담는 내부 DTO 입니다.
 */
@Getter
public class EarnablePointsInfoDto {
    private final boolean earnable;
    private final byte pointsToAward;

    private EarnablePointsInfoDto(boolean earnable, byte pointsToAward) {
        this.earnable = earnable;
        this.pointsToAward = pointsToAward;
    }

    public static EarnablePointsInfoDto earnable(byte points) {
        return new EarnablePointsInfoDto(true, points);
    }

    public static EarnablePointsInfoDto notEarnable() {
        return new EarnablePointsInfoDto(false, (byte) 0);
    }
}