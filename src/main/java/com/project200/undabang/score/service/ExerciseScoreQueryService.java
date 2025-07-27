package com.project200.undabang.score.service;

import com.project200.undabang.score.dto.response.EarnablePointsInfoResponseDto;

/**
 * 운동 기록과 관련된 점수 계산 및 조회를 담당하는 서비스입니다.
 */
public interface ExerciseScoreQueryService {

    /**
     * 현재 사용자의 예상 운동 점수 획득 정보를 조회합니다.
     *
     * @return 예상 획득 점수 정보 DTO
     */
    EarnablePointsInfoResponseDto getEarnablePointsInfo();
}
