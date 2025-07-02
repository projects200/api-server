package com.project200.undabang.policy.repository;

import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, PolicyKey> {
}
