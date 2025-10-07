package com.project200.undabang.member.repository.impl;

import com.project200.undabang.exercise.entity.QExercise;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.repository.MemberRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 주어진 회원 ID 목록에 해당하는 모든 회원 데이터를 비관적 잠금을 사용하여 조회합니다.
     *
     * @param sortedMemberIdList 조회할 회원의 고유 식별자 목록 (UUID 형식). ID는 정렬된 순서로 제공됩니다.
     * @return 비관적 잠금이 적용된 회원 엔티티 목록
     */
    @Override
    public List<Member> findAllByIdWithPessimisticLock(List<UUID> sortedMemberIdList) {
        QMember member = QMember.member;

        return queryFactory
                .selectFrom(member)
                .where(member.memberId.in(sortedMemberIdList))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }

    /**
     * 특정 회원이 지정된 기간 내에 수행한 운동 기록의 개수를 계산합니다.
     *
     * @param memberId 조회할 회원의 고유 식별자 (UUID 형식)
     * @param daysAgo  몇 일 전부터의 운동 기록을 조회할지 지정하는 날짜 수
     * @return 지정된 기간 동안의 회원 운동 기록 개수
     */
    @Override
    public Long countMemberExerciseInLastDays(UUID memberId, int daysAgo) {
        QExercise exercise = QExercise.exercise;

        LocalDate now = LocalDate.now();

        LocalDateTime exerciseStartDate = now.minusDays(daysAgo).atStartOfDay(); // N일전 00:00:00 부터 체크
        LocalDateTime endOfToday = now.plusDays(1).atStartOfDay(); // 내일 00:00:00 미만까지 체크(오늘 전체 포함)

        return queryFactory.select(exercise.count())
                .from(exercise)
                .where(
                        exercise.member.memberId.eq(memberId), // 특정 회원 조회
                        exercise.exerciseStartedAt.goe(exerciseStartDate), // 파라미터로 받은 기간 이후 수행한 운동
                        exercise.exerciseStartedAt.lt(endOfToday), // 오늘을 포함한 날짜까지 검색
                        exercise.exerciseDeletedAt.isNull() // 삭제하지 않은 운동만 검색
                )
                .fetchOne();
    }

    /**
     * 특정 회원이 현재 연도에 수행한 운동 일수(고유 날짜 기준)를 계산합니다.
     */
    @Override
    public Long countMemberExerciseInThisYear(UUID memberId) {
        QExercise exercise = QExercise.exercise;

        LocalDate now = LocalDate.now();

        LocalDateTime firstDateOfYear = now.withDayOfYear(1).atStartOfDay(); // 올해 1월 1일 00:00:00 부터 체크
        LocalDateTime endOfToday = now.plusDays(1).atStartOfDay(); // 내일 00:00:00 미만까지 체크(오늘 전체 포함)

        return queryFactory.select(exercise.exerciseStartedAt.dayOfYear().countDistinct())
                .from(exercise)
                .where(
                        exercise.member.memberId.eq(memberId), // 특정 회원 조회
                        exercise.exerciseStartedAt.goe(firstDateOfYear), // 올해 1월 1일부터 카운트
                        exercise.exerciseStartedAt.lt(endOfToday), // 오늘까지만 검색
                        exercise.exerciseDeletedAt.isNull() // 삭제하지 않은 운동만 검색
                )
                .fetchOne();
    }
}
