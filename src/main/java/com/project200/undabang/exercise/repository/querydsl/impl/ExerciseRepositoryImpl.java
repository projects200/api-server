package com.project200.undabang.exercise.repository.querydsl.impl;

import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.QExercise;
import com.project200.undabang.exercise.entity.QExercisePicture;
import com.project200.undabang.exercise.repository.querydsl.ExerciseRepositoryCustom;
import com.project200.undabang.member.entity.QMemberLocation;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Exercise 엔티티에 대한 QueryDSL 기반 커스텀 레포지토리 구현체입니다.
 * 운동 기록 조회와 관련된 복잡한 쿼리를 처리합니다.
 */
public class ExerciseRepositoryImpl extends QuerydslRepositorySupport implements ExerciseRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    /**
     * QueryDSL 쿼리 팩토리를 주입받는 생성자입니다.
     */
    public ExerciseRepositoryImpl(JPAQueryFactory queryFactory){
        super(Exercise.class);
        this.queryFactory = queryFactory;
    }
    /**
     * 특정 회원이 소유한 운동 기록이 존재하는지 확인합니다.
     */
    @Override
    public boolean existsByRecordIdAndMemberId(UUID memberId, Long recordId) {
        QExercise exercise = QExercise.exercise;

        Integer fetchOne = queryFactory
                .selectOne()
                .from(exercise)
                .where(exercise.id.eq(recordId)
                        .and(exercise.member.memberId.eq(memberId))
                        .and(exercise.exerciseDeletedAt.isNull()))
                .fetchFirst();


        return fetchOne != null;
    }
    /**
     * 특정 회원의 특정 운동 기록 상세 정보를 조회합니다.
     * 운동 기본 정보는 join을 통해 한 번에 조회하고, 관련 이미지 URL은 별도 쿼리로 조회합니다.
     */
    @Override
    public FindExerciseRecordResponseDto findExerciseByExerciseId(UUID memberId, Long recordId) {
        QExercise exercise = QExercise.exercise; // 인스턴스 생성
        QMemberLocation memberLocation = QMemberLocation.memberLocation;
        QExercisePicture exercisePicture = QExercisePicture.exercisePicture;
        QPicture picture = QPicture.picture;

        FindExerciseRecordResponseDto respDto = queryFactory
                .select(Projections.fields(FindExerciseRecordResponseDto.class,
                        exercise.exerciseTitle,
                        exercise.exerciseDetail,
                        exercise.exercisePersonalType,
                        exercise.exerciseStartedAt,
                        exercise.exerciseEndedAt,
                        memberLocation.memberLocationTitle))
                .from(exercise)
                .join(memberLocation).on(memberLocation.member.eq(exercise.member).and(memberLocation.memberLocationDeletedAt.isNull()))
                .where(exercise.member.memberId.eq(memberId),
                        exercise.id.eq(recordId),
                        exercise.exerciseDeletedAt.isNull())
                .fetchOne();

        // exercise 테이블에 운동기록 추가 시
/*        FindExerciseRecordResponseDto respDto = queryFactory
                .select(Projections.fields(FindExerciseRecordResponseDto.class,
                        exercise.exerciseTitle,
                        exercise.exerciseDetail,
                        exercise.exercisePersonalType,
                        exercise.exerciseStartedAt,
                        exercise.exerciseEndedAt,
                        exercise.exerciseLocate))
                .from(exercise)
                .where(exercise.member.memberId.eq(memberId),
                        exercise.id.eq(recordId),
                        exercise.exerciseDeletedAt.isNull())
                .fetchOne();*/

        if(respDto != null){
            List<String> urlList = queryFactory
                    .select(picture.pictureUrl)
                    .from(exercisePicture)
                    .join(exercisePicture.pictures, picture)
                    .where(exercisePicture.exercise.id.eq(recordId)
                            .and(picture.pictureDeletedAt.isNull()))
                    .fetch();

            if(urlList.isEmpty()){
                respDto.setExercisePictureUrls(Optional.empty());
            }else{
                respDto.setExercisePictureUrls(Optional.of(urlList));
            }
        }

        return respDto;
    }
}
