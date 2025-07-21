package com.project200.undabang.policy.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.policy.dto.record.PolicyGroupItemRecord;
import com.project200.undabang.policy.repository.PolicyGroupRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class PolicyGroupRepositoryImplTest {
    @Autowired
    private PolicyGroupRepository policyGroupRepository;

    @Test
    @DisplayName("모든 정책을 그룹 이름과 함께 조회한다")
    void findAllPoliciesWithGroupName(){
        // when
        List<PolicyGroupItemRecord> recordList = policyGroupRepository.findAllPoliciesWithGroupName();

        // then
        Assertions.assertThat(recordList).isNotNull();
        Assertions.assertThat(recordList).hasSize(8);
        Assertions.assertThat(recordList)
                .allMatch(item -> item.groupName() != null && !item.groupName().isEmpty());
    }
}