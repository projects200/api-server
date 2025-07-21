package com.project200.undabang.policy.repository.impl;

import com.project200.undabang.policy.dto.record.PolicyGroupItemRecord;
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
    public List<PolicyGroupItemRecord> findAllPoliciesWithGroupName() {
        QPolicy policy = QPolicy.policy;
        QPolicyGroup policyGroup = QPolicyGroup.policyGroup;
        QPolicyGroupMapping policyGroupMapping = QPolicyGroupMapping.policyGroupMapping;

        return jpaQueryFactory.select(
                Projections.constructor(PolicyGroupItemRecord.class,
                        policyGroup.name,
                        policy.policyKey.stringValue(),
                        policy.policyValue,
                        policy.policyUnit,
                        policy.policyDescription))
                .from(policyGroupMapping)
                .join(policyGroupMapping.policy, policy)
                .join(policyGroupMapping.policyGroup, policyGroup)
                .fetch();
    }
}
