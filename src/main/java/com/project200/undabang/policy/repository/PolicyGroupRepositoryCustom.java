package com.project200.undabang.policy.repository;

import com.project200.undabang.policy.dto.record.PolicyGroupItemRecord;

import java.util.List;

public interface PolicyGroupRepositoryCustom {
    List<PolicyGroupItemRecord> findAllPoliciesWithGroupName();
}
