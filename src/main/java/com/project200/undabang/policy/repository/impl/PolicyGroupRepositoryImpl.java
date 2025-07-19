package com.project200.undabang.policy.repository.impl;

import com.project200.undabang.policy.dto.record.PolicyItemRecord;
import com.project200.undabang.policy.entity.QPolicy;
import com.project200.undabang.policy.entity.QPolicyGroup;
import com.project200.undabang.policy.entity.QPolicyGroupMapping;
import com.project200.undabang.policy.repository.PolicyGroupRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PolicyGroupRepositoryImpl implements PolicyGroupRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<PolicyItemRecord> findPoliciesByGroupName(String groupName) {
        QPolicy policy = QPolicy.policy;
        QPolicyGroup policyGroup = QPolicyGroup.policyGroup;
        QPolicyGroupMapping policyGroupMapping = QPolicyGroupMapping.policyGroupMapping;

        return jpaQueryFactory.select(
                Projections.constructor(PolicyItemRecord.class,
                        policy.policyKey.stringValue(),
                        policy.policyValue,
                        policy.policyUnit,
                        policy.policyDescription))
                .from(policy).join(policy.groupMappings, policyGroupMapping)
                .join(policyGroupMapping.policyGroup, policyGroup)
                .where(policyGroup.name.eq(groupName))
                .fetch();
    }
}
