package com.project200.undabang.batch.provider;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.batch.item.database.orm.JpaQueryProvider;

public class MemberScoreQuerydslProvider implements JpaQueryProvider {
    private EntityManager entityManager;

    @Override
    public Query createQuery() {
        return null;
    }

    @Override
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
