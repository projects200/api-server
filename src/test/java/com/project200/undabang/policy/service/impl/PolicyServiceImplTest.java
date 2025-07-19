package com.project200.undabang.policy.service.impl;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.provider.PolicyProvider;
import com.project200.undabang.policy.strategy.PolicyStrategy;
import com.project200.undabang.policy.strategy.PolicyStrategyFinder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
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

    @Mock
    private PolicyStrategyFinder policyStrategyFinder;

    @Mock
    private PolicyStrategy policyStrategy;

    @Test
    @DisplayName("올바른 정책 키를 요청하면, 정책 값을 정수로 변환하여 성공적으로 반환한다")
    void getPolicyAsInt_Success() {
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
        int result = policyService.getPolicyAsInt(key);

        // then
        assertThat(result).isEqualTo(100); // 정수 3이 맞는지 확인
        then(policyProvider).should().getAllPoliciesAsMap();    // 정확히 1번 호출 확인
    }

    @Test
    @DisplayName("존재하지 않는 정책 키를 요청하면, POLICY_NOT_FOUND 예외를 던진다")
    void getPolicyAsInt_Fail_WhenKeyNotFound() {
        // given
        PolicyKey nonExistentKey = PolicyKey.EXERCISE_SCORE_MAX_POINTS;

        // policyProvider가 비어있는 Map을 반환한다고 가정
        given(policyProvider.getAllPoliciesAsMap()).willReturn(Map.of());

        // when & then
        // policyService.getPolicyAsInt(...)를 실행했을 때,
        // CustomException이 발생하는지, 그리고 그 예외의 ErrorCode가 POLICY_NOT_FOUND인지 검증합니다.
        assertThatThrownBy(() -> policyService.getPolicyAsInt(nonExistentKey))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POLICY_NOT_FOUND);

        // 예외가 발생하더라도, policyProvider의 메소드는 1번 호출되어야 합니다.
        then(policyProvider).should().getAllPoliciesAsMap();
    }

    @Test
    @DisplayName("특정 정책을 요청하면, 해당 정책과 관련된 내용들을 반환한다")
    void getPoliciesByGroupName_success(){
        // given
        String policyType = "exercise";
        Object expectedValue = "exercisePolicies";

        BDDMockito.given(policyStrategyFinder.findStrategy(policyType)).willReturn(policyStrategy);
        BDDMockito.given(policyStrategy.getPolicyValue()).willReturn(expectedValue);

        // when
        Object result = policyService.getPoliciesByGroupName(policyType);

        // then
        Assertions.assertThat(result).isEqualTo(expectedValue);

        BDDMockito.then(policyStrategyFinder).should().findStrategy(policyType);
        BDDMockito.then(policyStrategy).should().getPolicyValue();
    }

    @Test
    @DisplayName("존재하지 않는 정책을 요청하면, 예외를 반환한다")
    void getPoliciesByGroupName_failed_NotExistPolicyName(){
        // given
        String policyType = "notExistPolicy";
        BDDMockito.given(policyStrategyFinder.findStrategy(policyType)).willThrow(new CustomException(ErrorCode.POLICY_NOT_FOUND, "해당 전략을 찾을 수 없습니다."));

        // when
        Assertions.assertThatThrownBy(() -> policyService.getPoliciesByGroupName(policyType))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POLICY_NOT_FOUND);

        // then

        BDDMockito.then(policyStrategyFinder).should().findStrategy(policyType);
        BDDMockito.then(policyStrategy).shouldHaveNoInteractions();
    }
}