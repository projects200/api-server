package com.project200.undabang.policy.service.impl;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.provider.PolicyProvider;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.policy.strategy.PolicyStrategyFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final PolicyProvider policyProvider;
    private final PolicyStrategyFinder policyStrategyFinder;


    @Override
    public int getPolicyAsInt(PolicyKey key) {
        Map<PolicyKey, Policy> policies = policyProvider.getAllPoliciesAsMap();
        if (policies.containsKey(key)) {
            Policy policy = policies.get(key);
            return Integer.parseInt(policy.getPolicyValue());
        } else {
            log.error("존재하지 않는 정책 키에 대한 요청입니다. Key: {}", key);
            throw new CustomException(ErrorCode.POLICY_NOT_FOUND, "요청한 정책 키(" + key + ")가 존재하지 않습니다.");
        }
    }

    @Override
    public Object getPoliciesByType(String policyType) {
        return policyStrategyFinder.findStrategy(policyType).getPolicyValue();
    }
}
