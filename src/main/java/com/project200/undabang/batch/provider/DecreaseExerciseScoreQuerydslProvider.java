package com.project200.undabang.batch.provider;

import com.project200.undabang.exercise.entity.QExercise;
import com.project200.undabang.member.entity.QMember;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.Query;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;

import java.time.LocalDateTime;

/**
 * 운동 점수 감소 대상 회원을 조회하는 QueryDSL Provider 입니다.
 * 마지막 운동일이 기준일보다 오래되었거나, 운동 기록이 전혀 없는 회원을 조회합니다.
 * 기존 JpaQueryProvider 인터페이스로 구현하였으나, Spring Batch에서 제공하는 구현체가 있음을 알고 리팩토링 하였습니다.
 */
public class DecreaseExerciseScoreQuerydslProvider extends AbstractJpaQueryProvider {
    private final LocalDateTime referenceDate;

    public DecreaseExerciseScoreQuerydslProvider(LocalDateTime referenceDate) {
        this.referenceDate = referenceDate;
    }

    /**
     * 운동 점수 감소 대상 회원을 조회하는 JPA Query를 생성합니다.
     */
    @Override
    public Query createQuery() {
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(getEntityManager());
        QMember member = QMember.member;

        return jpaQueryFactory
                .selectFrom(member)
                .where(
                        checkLastExerciseDateIsBeforeReferenceDate(member)
                                .or(checkHasNoExerciseRecord(member))
                )
                .orderBy(member.memberId.asc())
                .createQuery();
    }

    /**
     * 회원의 마지막 운동일이 기준일보다 이전인지 확인하는 조건을 생성합니다.
     */
    private BooleanExpression checkLastExerciseDateIsBeforeReferenceDate(QMember member) {
        QExercise exercise = QExercise.exercise;

        return JPAExpressions
                .select(exercise.exerciseStartedAt.max())
                .from(exercise)
                .where(exercise.member.eq(member))
                .loe(this.referenceDate);
    }

    /**
     * 회원에게 운동 기록이 없는지 확인하는 조건을 생성합니다.
     */
    private BooleanExpression checkHasNoExerciseRecord(QMember member){
        QExercise exercise = QExercise.exercise;

        return JPAExpressions
                .selectFrom(exercise)
                .where(exercise.member.eq(member))
                .notExists();
    }

    /**
     * 속성 설정 후 실행되는 메서드입니다.
     * AbstractJpaQueryProvider를 상속받았기 때문에 구현이 필요하지만,
     * 이 클래스에서는 별도의 초기화 로직이 필요하지 않습니다.
     */
    @Override
    public void afterPropertiesSet() throws Exception {}
}
