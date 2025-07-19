package com.project200.undabang.policy.service.impl;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.policy.dto.record.PolicyItemRecord;
import com.project200.undabang.policy.dto.response.PolicyResponseDto;
import com.project200.undabang.policy.repository.PolicyGroupRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PolicyQueryServiceImplTest {
    @Mock
    private PolicyGroupRepository policyGroupRepository;

    @InjectMocks
    private PolicyQueryServiceImpl policyQueryService;

    @Test
    @DisplayName("정책 그룹 이름으로 조회 성공시 PolicyResponseDto를 반환해야 한다")
    void getPoliciesByGroupName() {
        // given
        String groupName = "exercise-score";
        List<PolicyItemRecord> mockPolicies = List.of(
                new PolicyItemRecord("KEY_1", "VALUE_1", "UNIT_1", "DESC_1"),
                new PolicyItemRecord("KEY_2", "VALUE_2", "UNIT_2", "DESC_2")
        );

        // policyGroupRepository.findPoliciesAsDtoByGroupName(groupName)이 호출되면,
        // 위에서 만든 mockPolicies 리스트를 반환하도록 설정
        given(policyGroupRepository.findPoliciesByGroupName(groupName)).willReturn(mockPolicies);

        // when
        PolicyResponseDto result = policyQueryService.getPoliciesByGroupName(groupName);

        // then
        Assertions.assertThat(result.getGroupName()).isEqualTo(groupName);
        Assertions.assertThat(result.getSize()).isEqualTo(2);
        Assertions.assertThat(result.getPolicies().get(0).policyKey()).isEqualTo("KEY_1");

        // policyGroupRepository의 findPoliciesAsDtoByGroupName 메서드가 1번 호출되었는지 검증
        verify(policyGroupRepository).findPoliciesByGroupName(groupName);
    }

    @Test
    @DisplayName("정책 그룹 이름으로 조회시, 이름이 없다면 CustomExecption을 반환한다")
    void getPoliciesByGroupName_ThrowsCustomException(){
        // given
        String groupName = "no-policy";

        given(policyGroupRepository.findPoliciesByGroupName(groupName)).willReturn(Collections.EMPTY_LIST);

        // when (then)
        Assertions.assertThatThrownBy(() -> policyQueryService.getPoliciesByGroupName(groupName))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POLICY_NOT_EXIST.getMessage());
    }

    @Test
    @DisplayName("정책 그룹 이름으로 조회시, null 이 반환되는 경우 CustomExecption을 반환한다")
    void getPoliciesByGroupName_IfNullReturnsThrowsCustomException(){
        // given
        String groupName = "no-policy";

        given(policyGroupRepository.findPoliciesByGroupName(groupName)).willReturn(null);

        // when (then)
        Assertions.assertThatThrownBy(() -> policyQueryService.getPoliciesByGroupName(groupName))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POLICY_NOT_EXIST.getMessage());
    }
}