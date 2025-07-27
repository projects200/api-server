package com.project200.undabang.policy.service.impl;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.provider.PolicyProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PolicyServiceImplTest {

    @InjectMocks
    private PolicyServiceImpl policyService;

    @Mock
    private PolicyProvider policyProvider;

    @Test
    @DisplayName("올바른 정책 키를 요청하면, 정책 값을 정수로 변환하여 성공적으로 반환한다")
    void getPolicyValueAsInt_Success() {
        // given (전제 조건 설정)
        PolicyKey key = PolicyKey.EXERCISE_SCORE_MAX_POINTS;
        Policy mockPolicy = Policy.builder()
                .policyKey(key)
                .policyValue("100")
                .policyUnit("POINTS")
                .build();
        Map<PolicyKey, Policy> mockPolicyMap = Map.of(key, mockPolicy);

        given(policyProvider.getAllPoliciesAsMap()).willReturn(mockPolicyMap);

        // when
        int result = policyService.getPolicyValueAsInt(key);

        // then
        assertThat(result).isEqualTo(100); // 정수 3이 맞는지 확인
        then(policyProvider).should().getAllPoliciesAsMap();    // 정확히 1번 호출 확인
    }

    @Test
    @DisplayName("존재하지 않는 정책 키를 요청하면, POLICY_NOT_FOUND 예외를 던진다")
    void getPolicyValueAsInt_Fail_WhenKeyNotFound() {
        // given
        PolicyKey nonExistentKey = PolicyKey.EXERCISE_SCORE_MAX_POINTS;

        // policyProvider가 비어있는 Map을 반환한다고 가정
        given(policyProvider.getAllPoliciesAsMap()).willReturn(Map.of());

        // when & then
        // policyService.getPolicyAsInt(...)를 실행했을 때,
        // CustomException이 발생하는지, 그리고 그 예외의 ErrorCode가 POLICY_NOT_FOUND인지 검증합니다.
        assertThatThrownBy(() -> policyService.getPolicyValueAsInt(nonExistentKey))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POLICY_NOT_FOUND);

        // 예외가 발생하더라도, policyProvider의 메소드는 1번 호출되어야 합니다.
        then(policyProvider).should().getAllPoliciesAsMap();
    }

}