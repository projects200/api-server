package com.project200.undabang.policy.service;

import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;

public interface PolicyService {

    /**
     * 캐시된 Map에서 특정 정책 값을 정수형으로 편리하게 조회하는 메소드입니다.
     * @param key 정책 키(policy_key)
     * @return 정책 값(policy_value)이 정수형으로 변환된 결과
     */
    int getPolicyValueAsInt(PolicyKey key);

    /**
     * 캐시된 Map에서 특정 정책 값을 바이트형으로 편리하게 조회하는 메소드입니다.
     *
     * @param key 정책 키(policy_key)
     * @return 정책 값(policy_value)이 바이트형으로 변환된 결과
     */
    byte getPolicyValueAsByte(PolicyKey key);

    /**
     * 캐시된 Map에서 특정 정책 값을 문자열로 조회하는 메소드입니다.
     *
     * @param key 정책 키(policy_key)
     * @return 정책 값(policy_value) 문자열 형태의 결과
     */
    String getPolicyValueAsString(PolicyKey key);

    /**
     * 캐시된 Map에서 특정 정책이 활성화되어 있는지 여부를 확인하는 메소드입니다.
     *
     * @param key 정책 키(policy_key)
     * @return 정책이 활성화되어 있으면 true, 비활성화되어 있으면 false
     */
    boolean isPolicyEnabled(PolicyKey key);

    /**
     * 캐시된 Map에서 특정 정책을 조회하는 메소드입니다.
     *
     * @param key 정책 키(policy_key)
     * @return 해당 정책 객체
     */
    Policy getPolicy(PolicyKey key);
}
