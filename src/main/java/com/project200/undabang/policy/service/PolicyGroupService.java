package com.project200.undabang.policy.service;

import com.project200.undabang.policy.dto.response.PolicyResponseDto;

import java.util.Optional;

public interface PolicyGroupService {
    /**
     * 그룹 이름으로 단일 정책 그룹 DTO를 조회합니다.
     * 캐시에 존재하면 캐시에서, 없으면 404 Not Found에 해당하므로 Optional.empty()를 반환합니다.
     * @param groupName 조회할 정책 그룹의 이름
     * @return Optional<PolicyResponseDto>
     */
    Optional<PolicyResponseDto> getByGroupName(String groupName);
}
