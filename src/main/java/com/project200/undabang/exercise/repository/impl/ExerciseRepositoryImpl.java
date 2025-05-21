package com.project200.undabang.exercise.repository.impl;

import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.dto.response.PictureDataResponse;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.QExercise;
import com.project200.undabang.exercise.entity.QExercisePicture;
import com.project200.undabang.exercise.repository.ExerciseRepositoryCustom;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
                .orderBy(exercise.id.asc())
                .fetchOne();

        if(respDto != null){
            List<PictureDataResponse> urlList = queryFactory
                    .select(Projections.fields(PictureDataResponse.class,
                            picture.id.as("pictureId"),
                            picture.pictureUrl,
                            picture.pictureName,
                            picture.pictureExtension))
                    .from(exercisePicture)
                    .join(exercisePicture.pictures, picture)
                    .where(exercisePicture.exercise.id.eq(recordId)
                            .and(picture.pictureDeletedAt.isNull()))
                    .fetch();

            respDto.setPictureDataList(urlList.isEmpty() ? Optional.empty() : Optional.of(urlList));
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

    /**
     * 특정 회원의 기간별 운동 기록 개수를 조회합니다.
     * 요청한 기간의 모든 날짜에 대해 운동 기록 개수를 반환하며, 기록이 없는 날짜는 0으로 채웁니다.
     */
    @Override
    public List<FindExerciseRecordByPeriodResponseDto> findExercisesByPeriod(UUID memberId, LocalDate startDate, LocalDate endDate) {
        QExercise exercise = QExercise.exercise;

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);

        List<LocalDate> dateList = new ArrayList<>();
        for (long i = 0; i <= daysBetween; i++) {
            dateList.add(startDate.plusDays(i));
        }

        NumberPath<Long> countRecord = Expressions.numberPath(Long.class, "exerciseCount");

        List<Tuple> result = queryFactory
                .select(exercise.exerciseStartedAt.year(),
                        exercise.exerciseStartedAt.month(),
                        exercise.exerciseStartedAt.dayOfMonth(),
                        exercise.count().as(countRecord))
                .from(exercise)
                .where(exercise.member.memberId.eq(memberId),
                        exercise.exerciseDeletedAt.isNull(),
                        exercise.exerciseStartedAt.goe(startDate.atStartOfDay()),
                        exercise.exerciseStartedAt.lt(endDate.plusDays(1).atStartOfDay()))
                .groupBy(exercise.exerciseStartedAt.year(),
                        exercise.exerciseStartedAt.month(),
                        exercise.exerciseStartedAt.dayOfMonth())
                .fetch();

        Map<LocalDate, Long> dateMap = new HashMap<>();

        for (Tuple tuple : result) {
            LocalDate date = LocalDate.of(
                    tuple.get(exercise.exerciseStartedAt.year()),
                    tuple.get(exercise.exerciseStartedAt.month()),
                    tuple.get(exercise.exerciseStartedAt.dayOfMonth())
            );
            dateMap.put(date, tuple.get(countRecord));
        }

        List<FindExerciseRecordByPeriodResponseDto> responseDtoList = new ArrayList<>();

        for (LocalDate localDate : dateList) {
            FindExerciseRecordByPeriodResponseDto dto = new FindExerciseRecordByPeriodResponseDto();
            dto.setDate(localDate);
            dto.setExerciseCount(dateMap.get(localDate));

            if(dateMap.containsKey(localDate)){
                responseDtoList.add(dto);
            }else{
                responseDtoList.add(new FindExerciseRecordByPeriodResponseDto(localDate, 0L));
            }
        }


        return responseDtoList;
    }
}
