package com.project200.undabang.policy.service.impl;

import com.project200.undabang.common.aop.LogExecutionTime;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.policy.dto.record.PolicyItemRecord;
import com.project200.undabang.policy.dto.response.PolicyResponseDto;
import com.project200.undabang.policy.repository.PolicyGroupRepository;
import com.project200.undabang.policy.service.PolicyQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyQueryServiceImpl implements PolicyQueryService {
    private final PolicyGroupRepository policyGroupRepository;

    @Override
    @LogExecutionTime
    public PolicyResponseDto getPoliciesByGroupNameFromDB(String groupName) {
        List<PolicyItemRecord> policies = policyGroupRepository.findPoliciesByGroupName(groupName);

        if(Objects.isNull(policies) || policies.isEmpty()){
            throw new CustomException(ErrorCode.POLICY_NOT_EXIST);
        }

       return PolicyResponseDto.builder()
                .groupName(groupName)
                .size(policies.size())
                .policies(policies)
                .build();
    }
}
