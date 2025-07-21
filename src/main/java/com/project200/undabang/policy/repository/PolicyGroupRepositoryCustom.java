package com.project200.undabang.policy.repository;

import com.project200.undabang.policy.dto.record.PolicyGroupItemRecord;
import com.project200.undabang.policy.dto.record.PolicyItemRecord;

import java.util.List;

public interface PolicyGroupRepositoryCustom {
    List<PolicyItemRecord> findPoliciesByGroupName(String groupName);
    List<PolicyGroupItemRecord> findAllPoliciesWithGroupName();
}
