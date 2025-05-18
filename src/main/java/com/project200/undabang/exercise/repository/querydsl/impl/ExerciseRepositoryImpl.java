package com.project200.undabang.exercise.repository.querydsl.impl;

import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.QExercise;
import com.project200.undabang.exercise.entity.QExercisePicture;
import com.project200.undabang.exercise.repository.querydsl.ExerciseRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        QExercise exercise = QExercise.exercise;
        QExercisePicture exercisePicture = QExercisePicture.exercisePicture;
        QPicture picture = QPicture.picture;

        FindExerciseRecordResponseDto respDto = queryFactory
                .select(Projections.fields(FindExerciseRecordResponseDto.class,
                        exercise.exerciseTitle,
                        exercise.exerciseDetail,
                        exercise.exercisePersonalType,
                        exercise.exerciseStartedAt,
                        exercise.exerciseEndedAt,
                        exercise.exerciseLocation))
                .from(exercise)
                .where(exercise.member.memberId.eq(memberId),
                        exercise.id.eq(recordId),
                        exercise.exerciseDeletedAt.isNull())
                .fetchOne();

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
    /**
     * 특정 회원의 특정 날짜에 해당하는 운동 기록을 조회합니다.
     * 해당 날짜의 자정(00:00:00)부터 다음 날 자정 직전까지의 운동 기록을 검색합니다.
     *
     * 썸네일이 없을수도 있으니 LeftJoin을 사용하였습니다.
     */
    @Override
    public Optional<List<FindExerciseRecordDateResponseDto>> findExerciseRecordByDate(UUID memberId, LocalDate date) {
        QExercise exercise = QExercise.exercise;
        QExercisePicture exercisePicture = QExercisePicture.exercisePicture;
        QPicture picture = QPicture.picture;

        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = startDate.plusDays(1);

        List<FindExerciseRecordDateResponseDto> respDtoList = queryFactory
                .select(Projections.fields(FindExerciseRecordDateResponseDto.class,
                        exercise.id.as("exerciseId"),
                        exercise.exerciseTitle,
                        exercise.exercisePersonalType,
                        exercise.exerciseStartedAt,
                        exercise.exerciseEndedAt,
                        picture.pictureUrl.as("pictureUrl")))
                .from(exercise).leftJoin(exercisePicture).on(exercisePicture.exercise.eq(exercise))
                .leftJoin(picture).on(exercisePicture.pictures.eq(picture)
                        .and(picture.pictureDeletedAt.isNull()))
                .where(
                        exercise.member.memberId.eq(memberId),
                        exercise.exerciseStartedAt.goe(startDate).and(exercise.exerciseStartedAt.lt(endDate)),
                        exercise.exerciseDeletedAt.isNull())
                .distinct() // 현재 대표사진이 없으므로 일단 distinct() 적용, 랜덤으로 1장의 사진 적용
                .fetch();


        if(respDtoList.isEmpty()){
            return Optional.empty();
        }else{
            return Optional.of(respDtoList);
        }
    }
}
