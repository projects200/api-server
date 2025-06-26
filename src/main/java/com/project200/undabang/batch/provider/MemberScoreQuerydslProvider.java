package com.project200.undabang.batch.provider;

import com.project200.undabang.exercise.entity.QExercise;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.QMember;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.batch.item.database.orm.JpaQueryProvider;

import java.time.LocalDateTime;
import java.util.UUID;

public class MemberScoreQuerydslProvider implements JpaQueryProvider {
    private EntityManager entityManager;

    @Override
    public Query createQuery() {
        QMember member = QMember.member;
        QExercise exercise = QExercise.exercise;

        LocalDateTime findMemberByPeriod = LocalDateTime.now().minusWeeks(2);

        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);

        // 서브쿼리를 작성해서 회원별로 가장 최근에 운동기록을 생성한 날짜를 찾음
        JPQLQuery<LocalDateTime> lastExerciseDate = JPAExpressions
                .select(exercise.exerciseStartedAt.max())
                .from(exercise)
                .where(exercise.member.eq(member));

        JPAQuery<UUID> query = jpaQueryFactory
                .select(member.memberId)
                .from(member)
                .where(lastExerciseDate.loe(lastExerciseDate));

        JPAQuery<Member> queryTest = jpaQueryFactory // memberId만 반환화는것과 전체 엔티티를 반환하기 위해 테스트 하기위한 코드
                .selectFrom(member)
                .where(lastExerciseDate.loe(lastExerciseDate));


        return query.createQuery();
    }

    @Override
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
