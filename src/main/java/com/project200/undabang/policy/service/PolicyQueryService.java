package com.project200.undabang.policy.service;

import com.project200.undabang.policy.dto.response.PolicyResponseDto;

public interface PolicyQueryService {
    PolicyResponseDto getPoliciesByGroupNameFromDB(String groupName);
}
