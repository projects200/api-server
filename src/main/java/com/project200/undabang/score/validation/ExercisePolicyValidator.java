package com.project200.undabang.score.validation;

import java.time.LocalDateTime;

/**
 * 운동 점수 정책과 관련된 유효성 검증을 담당하는 컴포넌트입니다.
 */
public interface ExercisePolicyValidator {

    /**
     * 운동 기록후 점수 획득이 가능한 유효 기간을 계산합니다.
     * 정책에 정의된 기간만큼 현재 시간에서 빼서 유효 종료 시간을 반환합니다.
     *
     * @return 유효 종료 시간
     */
    LocalDateTime calculateValidityEndDate();
}