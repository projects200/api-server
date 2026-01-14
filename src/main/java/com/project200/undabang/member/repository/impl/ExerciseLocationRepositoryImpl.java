package com.project200.undabang.member.repository.impl;

import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.exercise.entity.QExerciseType;
import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
import com.project200.undabang.member.dto.record.PreferredExerciseRecord;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.entity.QExerciseLocation;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.entity.QMemberPicture;
import com.project200.undabang.member.entity.QPreferredExercise;
import com.project200.undabang.member.repository.ExerciseLocationRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.set;

@Repository
@RequiredArgsConstructor
public class ExerciseLocationRepositoryImpl implements ExerciseLocationRepositoryCustom {
        private final JPAQueryFactory queryFactory;

        /**
         * 운동 장소를 보유한 회원들의 운동 장소와 및 회원 정보(성별, 생년월일, 썸네일 이미지, 대표 프로필 이미지)를 조회합니다.
         * 추후, 거리 및 다른 정보(성별, 선호운동, 운동점수 등...)들을 기반으로 필터링이 적용될 예정입니다.
         *
         * @return 회원의 운동 위치 및 프로필 정보를 담은 GetMembersExerciseLocationsResponse 객체의 리스트
         */
        @Override
        public List<GetMembersExerciseLocationsResponse> getMembersExerciseLocations(
                        Set<UUID> excludeMemberIdSet,
                        Double leftTopLatitude, Double leftTopLongitude,
                        Double rightBottomLatitude, Double rightBottomLongitude) {
                QMember member = QMember.member;
                QMemberPicture memberPicture = QMemberPicture.memberPicture;
                QExerciseLocation exerciseLocation = QExerciseLocation.exerciseLocation;
                QPicture picture = QPicture.picture;
                QPreferredExercise preferredExercise = QPreferredExercise.preferredExercise;
                QExerciseType exerciseType = QExerciseType.exerciseType;

                List<GetMembersExerciseLocationsResponse> responses = queryFactory
                                .from(exerciseLocation)
                                .join(exerciseLocation.member, member)
                                .leftJoin(memberPicture).on(member.memberPicture.id.eq(memberPicture.id))
                                .leftJoin(memberPicture.picture, picture)
                                .leftJoin(member.preferredExercises, preferredExercise)
                                .on(preferredExercise.preferredExerciseDeletedAt.isNull())
                                .leftJoin(preferredExercise.exercise, exerciseType)
                                .where(exerciseLocation.exerciseLocationDeletedAt.isNull() // 삭제된 운동장소 제외
                                                .and(member.memberDeletedAt.isNull()) // 탈퇴한 회원 제외
                                                .and(member.memberId.notIn(excludeMemberIdSet)) // 내가 차단하거나 나를 차단한 사람들
                                                                                                // 제외
                                                .and(Expressions.numberTemplate(Double.class, "ST_X({0})",
                                                                exerciseLocation.exerciseLocationPoint)
                                                                .between(rightBottomLatitude, leftTopLatitude))
                                                .and(Expressions.numberTemplate(Double.class, "ST_Y({0})",
                                                                exerciseLocation.exerciseLocationPoint)
                                                                .between(leftTopLongitude, rightBottomLongitude)))
                                .transform(
                                                groupBy(member.memberId).list(
                                                                Projections.constructor(
                                                                                GetMembersExerciseLocationsResponse.class,
                                                                                member.memberId,
                                                                                memberPicture.memberPicturesUrl, // 썸네일
                                                                                                                 // 사진
                                                                                                                 // url
                                                                                picture.pictureUrl, // 이미지 사진 url
                                                                                member.memberNickname,
                                                                                member.memberGender,
                                                                                member.memberBday,
                                                                                member.memberScore,
                                                                                set(
                                                                                                Projections.constructor(
                                                                                                                ExerciseLocationRecord.class,
                                                                                                                exerciseLocation.exerciseLocationName,
                                                                                                                Expressions.numberTemplate(
                                                                                                                                Double.class,
                                                                                                                                "ST_X({0})",
                                                                                                                                exerciseLocation.exerciseLocationPoint), // 위도
                                                                                                                                                                         // 좌표
                                                                                                                                                                         // 매핑
                                                                                                                Expressions.numberTemplate(
                                                                                                                                Double.class,
                                                                                                                                "ST_Y({0})",
                                                                                                                                exerciseLocation.exerciseLocationPoint) // 경도
                                                                                                                                                                        // 좌표
                                                                                                                                                                        // 매핑
                                                                                                )),
                                                                                set(
                                                                                                Projections.constructor(
                                                                                                                PreferredExerciseRecord.class,
                                                                                                                exerciseType.exerciseName,
                                                                                                                preferredExercise.preferredExerciseDate,
                                                                                                                preferredExercise.preferredExerciseSkillLevel)))));

                // 빈 선호 운동 제거
                responses.forEach(response -> {
                        if (response.getPreferredExercises() != null) {
                                response.getPreferredExercises().removeIf(record -> record.exerciseName() == null);
                        }
                });

                return responses;
        }
}