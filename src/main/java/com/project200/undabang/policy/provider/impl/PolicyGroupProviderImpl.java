package com.project200.undabang.policy.provider.impl;

import com.project200.undabang.policy.dto.record.PolicyGroupItemRecord;
import com.project200.undabang.policy.dto.record.PolicyItemRecord;
import com.project200.undabang.policy.dto.response.PolicyResponseDto;
import com.project200.undabang.policy.provider.PolicyGroupProvider;
import com.project200.undabang.policy.repository.PolicyGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 정책 그룹 정보를 제공하는 프로바이더 구현 클래스입니다.
 * 데이터베이스에서 정책 정보를 조회하고 캐싱 처리를 담당합니다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyGroupProviderImpl implements PolicyGroupProvider {
    private final PolicyGroupRepository policyGroupRepository;

    /**
     * 모든 정책 그룹 정보를 조회하여 그룹 이름을 키로 하는 맵으로 반환합니다.
     * {@code @Cacheable("policy-group")} 어노테이션을 통해 조회된 결과는 캐시됩니다.
     *
     * @return 그룹 이름을 키로, 해당 그룹의 정책 정보를 값으로 갖는 Map
     */
    @Override
    @Cacheable("policy-group")
    public Map<String, PolicyResponseDto> getAllPolicyGroupAsMap() {
        List<PolicyGroupItemRecord> recordList = policyGroupRepository.findAllPoliciesWithGroupName();
        Map<String, PolicyResponseDto> policyGroupMap = new HashMap<>();

        // groupName을 키로 하는 DTO 조회
        // 없다면 람다식을 실행하여 새로운 DTO를 만들고 맵에 추가후 반환 (이미 존재하면 기존 DTO 반환
        for (PolicyGroupItemRecord record : recordList) {
            PolicyResponseDto policyResponseDto = policyGroupMap.computeIfAbsent(record.groupName(), key ->
                    PolicyResponseDto.builder()
                            .groupName(key)
                            .size(0)
                            .policies(new ArrayList<>())
                            .build()
            );

            policyResponseDto.addPolicyItem(PolicyItemRecord.builder()
                            .policyKey(record.policyKey())
                            .policyValue(record.policyValue())
                            .policyUnit(record.policyUnit())
                            .policyDescription(record.policyDescription())
                            .build());
        }

        return policyGroupMap;
    }

    /**
     * "policy-group" 캐시에 저장된 모든 정책 정보를 제거합니다.
     * {@code @CacheEvict} 어노테이션을 사용하여 캐시를 비웁니다.
     * 이 메소드는 정책 정보가 변경되었을 때 캐시를 갱신하기 위해 호출될 수 있습니다.
     */
    @Override
    @CacheEvict(value = "policy-group", allEntries = true)
    public void refreshPolicies() {

    }
}
