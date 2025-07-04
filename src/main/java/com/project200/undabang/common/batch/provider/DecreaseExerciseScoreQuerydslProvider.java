package com.project200.undabang.common.batch.provider;

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
    private JPAQueryFactory jpaQueryFactory;

    public DecreaseExerciseScoreQuerydslProvider(LocalDateTime referenceDate) {
        this.referenceDate = referenceDate;
    }

    /**
     * 운동 점수 감소 대상 회원을 조회하는 JPA Query를 생성합니다.
     */
    @Override
    public Query createQuery() {
        QMember member = QMember.member;

        return jpaQueryFactory
                .selectFrom(member)
                .where(
                        member.memberDeletedAt.isNull().and(
                                checkLastExerciseDateIsBeforeReferenceDate(member)
                                        .or(checkHasNoExerciseRecord(member))
                        )
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
                .where(exercise.member.eq(member)
                        .and(exercise.exerciseDeletedAt.isNull()))
                .loe(referenceDate);
    }

    /**
     * 운동기록이 없는 회원이 가입일 기준 유예기간이 지났는지 확인하는 조건을 생성합니다.
     * 운동기록이 없고, 멤버 생성일이 기준일 이전인 조건
     */
    private BooleanExpression checkHasNoExerciseRecord(QMember member){
        QExercise exercise = QExercise.exercise;

        return JPAExpressions
                .selectFrom(exercise)
                .where(exercise.member.eq(member)
                        .and(exercise.exerciseDeletedAt.isNull()))
                .notExists()
                .and(member.memberCreatedAt.loe(referenceDate));
    }

    /**
     * 속성 설정 후 실행되는 메서드입니다.
     * AbstractJpaQueryProvider를 상속받았기 때문에 구현이 필요하지만,
     * 이 클래스에서는 별도의 초기화 로직이 필요하지 않습니다.
     *
     * 특별히 추가할 검증 로직이 없다면, 메소드의 내부를 비워도 좋다.
     * 모든 의존성 주입이 끝난 후, Bean 이 사용되기 직전에 호출될 초기화 로직임
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // EntityManager 가 주입된 이후에 JPAQueryFactory를 생성
        this.jpaQueryFactory = new JPAQueryFactory(getEntityManager());
    }
}
