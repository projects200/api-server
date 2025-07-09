package com.project200.undabang.policy.strategy;

import com.project200.undabang.policy.dto.response.ExercisePolicyResponseDto;
import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.provider.PolicyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * '운동 점수' 관련 정책을 조회하는 전략 구현체입니다.
 * {@link PolicyStrategy} 인터페이스를 구현하며, 'exercise-score' 유형의 정책 요청을 처리합니다.
 * {@link PolicyProvider}를 통해 운동 및 점수와 관련된 모든 정책 값을 조회하고,
 * 이를 {@link ExercisePolicyResponseDto}로 변환하여 반환합니다.
 */
@Component
@RequiredArgsConstructor
public class ExercisePolicyStrategy implements PolicyStrategy {
    private final PolicyProvider policyProvider;

    /**
     * 이 전략이 처리할 정책의 유형을 반환합니다.
     * 이 값은 {@link PolicyStrategyFinder}가 적절한 전략을 찾는 데 사용하는 키가 됩니다.
     */
    @Override
    public String getPolicyType() {
        return "exercise-score";
    }

    /**
     * 운동 점수 관련 정책 값들을 조회하여 DTO 형태로 반환합니다.
     * {@link PolicyProvider}에서 관련 정책들을 모두 가져와
     * {@link ExercisePolicyResponseDto} 객체를 생성하고 필드를 채워서 반환합니다.
     */
    @Override
    public Object getPolicyValue() {
        Map<PolicyKey, Policy> policyMap =  policyProvider.getAllPoliciesAsMap();

        return ExercisePolicyResponseDto.builder()
                .maxPoint(policyMap.get(PolicyKey.EXERCISE_SCORE_MAX_POINTS).getPolicyValue())
                .minPoint(policyMap.get(PolicyKey.EXERCISE_SCORE_MIN_POINTS).getPolicyValue())
                .initialPoint(policyMap.get(PolicyKey.SIGNUP_INITIAL_POINTS).getPolicyValue())
                .penaltyPoint(policyMap.get(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS).getPolicyValue())
                .penaltyThresholdDay(policyMap.get(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS).getPolicyValue())
                .pointPerExercise(policyMap.get(PolicyKey.POINTS_PER_EXERCISE).getPolicyValue())
                .validityPeriod(policyMap.get(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD).getPolicyValue())
                .build();
    }
}
