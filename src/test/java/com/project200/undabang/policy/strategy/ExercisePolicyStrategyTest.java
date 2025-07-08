package com.project200.undabang.policy.strategy;

import com.project200.undabang.policy.dto.response.ExercisePolicyResponseDto;
import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.provider.PolicyProvider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ExercisePolicyStrategyTest {
    @InjectMocks
    private ExercisePolicyStrategy exercisePolicyStrategy;

    @Mock
    private PolicyProvider policyProvider;

    @Test
    @DisplayName("getPolicyType()은 exerecises를 반환한다")
    void getPolicyType() {
        String policyType = exercisePolicyStrategy.getPolicyType();

        Assertions.assertThat(policyType).isEqualTo("exercises");
    }

    @Test
    @DisplayName("getPolicyValue()는 PolicyProvider로 부터 받은 정책 값으로 DTO를 생성하여 점수 정책 관련을 반환한다")
    void getPolicyValue() {
        // given
        Map<PolicyKey, Policy> mockPolicyMap = new HashMap<>();

        addMockPolicy(mockPolicyMap, PolicyKey.EXERCISE_SCORE_MAX_POINTS, "100");
        addMockPolicy(mockPolicyMap, PolicyKey.EXERCISE_SCORE_MIN_POINTS, "0");
        addMockPolicy(mockPolicyMap, PolicyKey.SIGNUP_INITIAL_POINTS, "35");
        addMockPolicy(mockPolicyMap, PolicyKey.PENALTY_SCORE_DECREMENT_POINTS, "1");
        addMockPolicy(mockPolicyMap, PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS, "7");
        addMockPolicy(mockPolicyMap, PolicyKey.POINTS_PER_EXERCISE, "3");
        addMockPolicy(mockPolicyMap, PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD, "2");

        BDDMockito.given(policyProvider.getAllPoliciesAsMap()).willReturn(mockPolicyMap);

        // when
        Object result = exercisePolicyStrategy.getPolicyValue();

        // then
        Assertions.assertThat(result).isInstanceOf(ExercisePolicyResponseDto.class);
        ExercisePolicyResponseDto responseDto = (ExercisePolicyResponseDto) result;

        Assertions.assertThat(responseDto.getMaxPoint()).isEqualTo("100");
        Assertions.assertThat(responseDto.getMinPoint()).isEqualTo("0");
        Assertions.assertThat(responseDto.getInitialPoint()).isEqualTo("35");
        Assertions.assertThat(responseDto.getPenaltyPoint()).isEqualTo("1");
        Assertions.assertThat(responseDto.getPenaltyThresholdDay()).isEqualTo("7");
        Assertions.assertThat(responseDto.getPointPerExercise()).isEqualTo("3");
        Assertions.assertThat(responseDto.getValidityPeriod()).isEqualTo("2");
    }

    private void addMockPolicy(Map<PolicyKey, Policy> map, PolicyKey key, String value) {
        Policy mockPolicy = Mockito.mock(Policy.class);
        BDDMockito.given(mockPolicy.getPolicyValue()).willReturn(value);
        map.put(key, mockPolicy);
    }
}