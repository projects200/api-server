package com.project200.undabang.policy.repository;

import com.project200.undabang.policy.entity.PolicyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyGroupRepository extends JpaRepository<PolicyGroup, Integer>, PolicyGroupRepositoryCustom {
}
