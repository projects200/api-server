package com.project200.undabang.policy.strategy;

import com.project200.undabang.policy.dto.response.ExercisePolicyResponseDto;
import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.provider.PolicyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExercisePolicyStrategyImpl implements PolicyStrategy {
    private final PolicyProvider policyProvider;

    @Override
    public String getPolicyType() {
        return "exercises";
    }

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
