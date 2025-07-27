package com.project200.undabang.policy.service.impl;

import com.project200.undabang.common.aop.LogExecutionTime;
import com.project200.undabang.policy.dto.record.PolicyGroupItemRecord;
import com.project200.undabang.policy.dto.record.PolicyItemRecord;
import com.project200.undabang.policy.dto.response.PolicyResponseDto;
import com.project200.undabang.policy.repository.PolicyGroupRepository;
import com.project200.undabang.policy.service.PolicyGroupService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyGroupServiceImpl implements PolicyGroupService {
    private final PolicyGroupRepository policyGroupRepository;
    private final CacheManager cacheManager;

    @Override
    @Cacheable(value="policyGroups", key = "#groupName")
    @LogExecutionTime
    public Optional<PolicyResponseDto> getByGroupName(String groupName) {
        // 캐시에 값이 없는 경우 Optional.empty() 를 캐싱하고 반환함
        // 이 로직을 통해 캐시 미스 상황을 안전하게 처리
        log.warn("CacheMiss for policyGroups: '{}'. 이 그룹은 존재하지 않습니다", groupName);
        return Optional.empty();
    }

    /**
     * 어플리케이션 시작시 모든 정책을 캐시에 미리 저장합니다.
     */
    @PostConstruct
    public void loadAllPoliciesIntoCache() {
        log.info("==== 정책 그룹 캐시 Warming 시작 ====");

        try{
            // DB에서 모든 정책 그룹 아이템을 조회
            List<PolicyGroupItemRecord> recordList = policyGroupRepository.findAllPoliciesWithGroupName();

            // 조회된 값들을 그룹별 PolicyResponseDto로 가공
            Map<String, PolicyResponseDto> policyGroupMap = buildPolicyGroupMap(recordList);

            // 가공된 맵을 캐시에 저장
            populateCache(policyGroupMap);

            log.info("==== 정책 그룹 캐시 Warming 종료 ====");
        }catch (Exception e){
            log.error("==== 정책 그룹 캐시 Warming 실패 ====\n ====정책 그룹 캐시 예열에 실패했습니다.====", e.getMessage(), e);
        }
    }

    /**
     * 정책 아이템 리스트를 받아 그룹 이름(String)을 키로 하는 Map<String, PolicyResponseDto> 형태로 가공합니다.
     * @param recordList DB에서 조회한 모든 정책 아이템 레코드 리스트
     * @return 그룹별로 정리된 정책 DTO 맵
     */
    private Map<String, PolicyResponseDto> buildPolicyGroupMap(List<PolicyGroupItemRecord> recordList) {
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
     * 가공된 정책 DTO 맵을 'policyGroups' 캐시에 저장합니다.
     * @param policyGroupMap 캐시에 저장할 정책 DTO 맵
     */
    private void populateCache(Map<String, PolicyResponseDto> policyGroupMap) {
        Cache policyCache = cacheManager.getCache("policyGroups");
//        if(Objects.nonNull(policyCache)){
//            policyGroupMap.forEach((groupName, dto) -> policyCache.put(groupName, Optional.of(dto)));
//        }else{
//            log.error("==== 정책 그룹 캐시 조회 실패. 캐싱이 수행되지 않았습니다. ====");
//        }
        policyGroupMap.forEach((groupName, dto) -> policyCache.put(groupName, Optional.of(dto)));
    }
}