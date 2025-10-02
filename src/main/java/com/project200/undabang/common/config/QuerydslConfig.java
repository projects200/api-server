package com.project200.undabang.common.config;

import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuerydslConfig {
    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory(){
        // querydsl 5.1.0 (최신버전) 이 hibernate 6 (Spring 3.X.X) 버전과 호환되지 않아서 JPQLTemplates.DEFAULT 를 추가.
        return new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager);
    }
}
