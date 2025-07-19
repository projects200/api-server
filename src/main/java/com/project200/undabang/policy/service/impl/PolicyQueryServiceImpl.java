package com.project200.undabang.policy.service.impl;

import com.project200.undabang.policy.dto.response.PolicyResponseDto;
import com.project200.undabang.policy.repository.PolicyRepository;
import com.project200.undabang.policy.service.PolicyQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyQueryServiceImpl implements PolicyQueryService {
//    private final PolicyProviderImpl policyProvider;
    private final PolicyRepository policyRepository;

    @Override
    public PolicyResponseDto getPoliciesByGroupName(String groupName) {


        return null;
    }
}
