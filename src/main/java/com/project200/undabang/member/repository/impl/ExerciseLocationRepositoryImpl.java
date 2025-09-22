package com.project200.undabang.member.repository.impl;

import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.member.dto.record.MemberProfileAndLocationRecord;
import com.project200.undabang.member.entity.QExerciseLocation;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.entity.QMemberPicture;
import com.project200.undabang.member.repository.ExerciseLocationRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ExerciseLocationRepositoryImpl implements ExerciseLocationRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * 사용자의 운동 위치 및 프로필 정보를 조회합니다.
     *
     * @return 사용자의 운동 위치 및 프로필 정보를 담은 MemberProfileAndLocationRecord의 리스트
     */
    @Override
    public List<MemberProfileAndLocationRecord> getMembersExerciseLocations() {
        QMember member = QMember.member;
        QMemberPicture memberPicture = QMemberPicture.memberPicture;
        QExerciseLocation exerciseLocation = QExerciseLocation.exerciseLocation;
        QPicture picture = QPicture.picture;

        return queryFactory
                .select(Projections.constructor(MemberProfileAndLocationRecord.class,
                        member.memberId,
                        member.memberNickname,
                        member.memberGender,
                        member.memberBday,
                        picture.pictureUrl, // Todo : 썸네일 개발 후에는 memberPicture.url 써야함
                        exerciseLocation.exerciseLocationName,
                        exerciseLocation.exerciseLocationPoint
                ))
                .from(exerciseLocation)
                .join(exerciseLocation.member, member)
                .leftJoin(member.memberPicture, memberPicture)
                .leftJoin(memberPicture.picture, picture)
                .where(exerciseLocation.exerciseLocationDeletedAt.isNull() // 삭제된 운동장소 제외
                        .and(member.memberDeletedAt.isNull())) // 탈퇴한 회원 제외
                .fetch();
    }
}
