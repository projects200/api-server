package com.project200.undabang.score.service;

import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.score.dto.internal.EarnablePointsInfoDto;

import java.time.LocalDateTime;

/**
 * 운동 기록과 관련된 점수 계산 및 부여를 담당하는 서비스입니다.
 */
public interface ExerciseScoreCommandService {

    /**
     * 운동 기록 생성을 기반으로 사용자에게 점수를 부여합니다.
     * 이 메소드는 별도의 트랜잭션으로 동작하며, 실패하더라도 운동 기록 생성에는 영향을 주지 않습니다.
     *
     * @param exercise 새로 생성된 운동 기록 엔티티
     * @return 획득한 점수. 점수를 획득하지 못한 경우 0을 반환합니다.
     */
    byte awardPointsForExercise(Exercise exercise);

    /**
     * 특정 운동 기록에 대한 점수 획득 가능 여부와 점수를 계산합니다.
     * 이 메서드는 외부 API에서도 재사용될 수 있습니다.
     *
     * @param member            점수를 계산할 회원
     * @param exerciseStartedAt 운동 시작 시간
     * @return 점수 획득 정보 DTO
     */
    EarnablePointsInfoDto checkEarnablePoints(Member member, LocalDateTime exerciseStartedAt);
}

