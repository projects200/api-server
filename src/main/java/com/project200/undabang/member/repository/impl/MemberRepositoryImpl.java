package com.project200.undabang.member.repository.impl;

import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.exercise.entity.QExercise;
import com.project200.undabang.exercise.entity.QExerciseType;
import com.project200.undabang.member.dto.record.MemberProfileRecord;
import com.project200.undabang.member.dto.record.PreferredExerciseRecord;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.entity.QMemberPicture;
import com.project200.undabang.member.entity.QPreferredExercise;
import com.project200.undabang.member.repository.MemberRepositoryCustom;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<MemberProfileRecord> findMemberProfileWithPreferredExerciseActiveByMemberId(UUID memberId) {
        QMember member = QMember.member;
        QMemberPicture memberPicture = QMemberPicture.memberPicture;
        QPicture picture = QPicture.picture;
        QPreferredExercise preferredExercise = QPreferredExercise.preferredExercise;
        QExerciseType exerciseType = QExerciseType.exerciseType;

        Map<UUID, MemberProfileRecord> result = queryFactory.from(member)
                .leftJoin(member.memberPicture, memberPicture)
                .leftJoin(memberPicture.picture, picture)
                .leftJoin(member.preferredExercises, preferredExercise).on(preferredExercise.preferredExerciseDeletedAt.isNull())
                .leftJoin(preferredExercise.exercise, exerciseType)
                .where(member.memberId.eq(memberId),
                        member.memberDeletedAt.isNull()
                )
                .transform(
                        GroupBy.groupBy(member.memberId)
                                .as(
                                        Projections.constructor(MemberProfileRecord.class,
                                                member.memberId,
                                                member.memberNickname,
                                                member.memberDesc,
                                                member.memberGender,
                                                member.memberBday,
                                                picture.pictureUrl,
                                                member.memberPicture.memberPicturesUrl,
                                                member.memberScore,
                                                GroupBy.list(
                                                        Projections.constructor(PreferredExerciseRecord.class,
                                                                preferredExercise.id,
                                                                exerciseType.exerciseName,
                                                                preferredExercise.preferredExerciseSkillLevel,
                                                                preferredExercise.preferredExerciseDate,
                                                                exerciseType.exerciseTypeImageUrl
                                                        ).skipNulls() // 선호운동이 없는 회원은 null 대신 빈 리스트를 넣도록 함
                                                )
                                        )
                                )
                );

        return Optional.ofNullable(result.get(memberId));
    }

    /**
     * 회원의 프로필 이미지 정보를 포함한 상세 정보를 조회합니다.
     * 주어진 회원 ID를 기준으로 회원 정보를 검색하며, 해당 회원의 프로필 사진이
     * 관계형 매핑에 따라 포함되어 함께 반환됩니다.
     *
     * @param memberId 조회할 회원의 고유 식별자 (UUID 형식)
     * @return 주어진 회원 ID와 일치하는 회원 정보를 포함한 Optional 객체
     * 반환된 객체가 비어있을 경우 해당 ID에 해당하는 회원 정보가 존재하지 않음을 나타냅니다.
     */
    @Override
    public Optional<Member> findMemberWithProfileImage(UUID memberId) {
        QMember member = QMember.member;
        QMemberPicture memberPicture = QMemberPicture.memberPicture;
        QPicture picture = QPicture.picture;

        Member result = queryFactory.selectFrom(member)
                .leftJoin(member.memberPicture, memberPicture).fetchJoin()
                .leftJoin(memberPicture.picture, picture).fetchJoin()
                .where(member.memberId.eq(memberId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 주어진 회원 ID 목록에 대해 비관적 락(Pessimistic Lock)을 설정하여 회원 정보를 조회합니다.
     * 회원 테이블의 특정 튜플(파라미터로 전달받음)에 대해서 베타적인 쓰기 락을 적용합니다.
     * 베타적인 쓰기 락이 걸리면 다른 트랜잭션은 해당 데이터에 대해서 읽기 락을 거는것도 불가능합니다.
     *
     * @param sortedMemberIdList 조회할 회원 ID(UUID) 목록, 정렬된 상태여야 합니다
     * @return 비관적 락이 설정된 상태의 회원 정보 목록
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
