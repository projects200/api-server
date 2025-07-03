package com.project200.undabang.policy.provider.impl;

import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.repository.PolicyRepository;
import com.project200.undabang.policy.provider.PolicyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyProviderImpl implements PolicyProvider {
    private final PolicyRepository policyRepository;

    @Override
    @Cacheable("policies")
    public Map<PolicyKey, Policy> getAllPoliciesAsMap() {
        return policyRepository.findAll().stream()
                .collect(Collectors.toMap(Policy::getPolicyKey, Function.identity()));
    }

    @Override
    @CacheEvict(value = "policies", allEntries = true)
    public void refreshPolicies() {
        // 캐시 무효화 로직(추가적인 처리 필요 없음)
    }
}
