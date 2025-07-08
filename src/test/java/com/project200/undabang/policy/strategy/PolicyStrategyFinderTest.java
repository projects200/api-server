package com.project200.undabang.policy.strategy;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class PolicyStrategyFinderTest {
    private PolicyStrategyFinder policyStrategyFinder;

    @Mock
    private PolicyStrategy firstPolicyStrategy;

    @Mock
    private PolicyStrategy secondPolicyStrategy;

    @Mock
    private PolicyStrategy thirdPolicyStrategy;

    @BeforeEach
    void setUp(){
        BDDMockito.given(firstPolicyStrategy.getPolicyType()).willReturn("firstPolicyType");
        BDDMockito.given(secondPolicyStrategy.getPolicyType()).willReturn("secondPolicyType");
        BDDMockito.given(thirdPolicyStrategy.getPolicyType()).willReturn("thirdPolicyType");

        List<PolicyStrategy> policyStrategyList = List.of(firstPolicyStrategy, secondPolicyStrategy, thirdPolicyStrategy);
        policyStrategyFinder = new PolicyStrategyFinder(policyStrategyList);
    }

    @Test
    @DisplayName("존재하는 정책 타입을 입력하면, 해당하는 정책전략을 반환함")
    void findStrategy_success(){
        // given
        String policyType = "firstPolicyType";

        // when
        PolicyStrategy expectedPolicyStrategy = policyStrategyFinder.findStrategy(policyType.toLowerCase());

        // then
        Assertions.assertThat(expectedPolicyStrategy).isEqualTo(firstPolicyStrategy);
    }

    @Test
    @DisplayName("존재하지 않는 정책 타입을 요청하면, INVALID_INPUT 에러를 반환한다")
    void findStrategy_failed_policyNotExist(){
        // given
        String policyType = "invalidPolicyType";

        // then
        Assertions.assertThatThrownBy(() -> policyStrategyFinder.findStrategy(policyType))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POLICY_NOT_FOUND);
    }
}