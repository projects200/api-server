package com.project200.undabang.policy.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.policy.dto.record.PolicyItemRecord;
import com.project200.undabang.policy.repository.PolicyGroupRepository;
import jakarta.persistence.EntityManager;
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

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("그룹 이름으로 정책 조회시 DTO 리스트를 반환한다")
    void findPoliciesByGroupName(){
        // given
        String groupName = "exercise-score";

        // when
        List<PolicyItemRecord> recordList = policyGroupRepository.findPoliciesByGroupName(groupName);

        // then
        Assertions.assertThat(recordList).isNotNull();
        Assertions.assertThat(recordList).hasSize(8);
    }

    @Test
    @DisplayName("그룹 이름으로 정책 조회시 정책이 없다면 빈 리스트를 반환한다")
    void returnEmptyListWhenNoPolicyFound(){
        // given
        String groupName = "no-policy";

        // when
        List<PolicyItemRecord> recordList = policyGroupRepository.findPoliciesByGroupName(groupName);

        // then
        Assertions.assertThat(recordList).isNotNull();
        Assertions.assertThat(recordList).isEmpty();
    }
}