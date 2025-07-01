package com.project200.undabang.batch.provider;

import com.project200.undabang.exercise.entity.QExercise;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.QMember;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.batch.item.database.orm.JpaQueryProvider;

import java.time.LocalDateTime;

public class DecreaseExerciseScoreQuerydslProvider_deleted implements JpaQueryProvider {
    private EntityManager entityManager;
    private final LocalDateTime referenceDate;

    public DecreaseExerciseScoreQuerydslProvider_deleted(LocalDateTime referenceDate) {
        this.referenceDate = referenceDate;
    }

    @Override
    public Query createQuery() {
        QMember member = QMember.member;
        QExercise exercise = QExercise.exercise;

        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);

        JPAQuery<Member> query = jpaQueryFactory
                .selectFrom(member)
                .where(
                        JPAExpressions // 마지막 운동이 2주보다 오래된 회원
                                .select(exercise.exerciseStartedAt.max())
                                .from(exercise)
                                .where(exercise.member.eq(member))
                                .loe(referenceDate)
                        .or(
                                JPAExpressions // 회원 가입 후 운동 기록이 없는 회원
                                        .selectFrom(exercise)
                                        .where(exercise.member.eq(member))
                                        .notExists()
                        )
                )
                .orderBy(member.memberId.asc());

        return query.createQuery();
    }

    @Override
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
