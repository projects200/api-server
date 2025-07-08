package com.project200.undabang.policy.service;

import com.project200.undabang.policy.entity.PolicyKey;

public interface PolicyService {

    /**
     * 캐시된 Map에서 특정 정책 값을 정수형으로 편리하게 조회하는 메소드입니다.
     * @param key 정책 키(policy_key)
     * @return 정책 값(policy_value)이 정수형으로 변환된 결과
     */
    int getPolicyAsInt(PolicyKey key);
    Object getPoliciesByType(String policyType);
}
