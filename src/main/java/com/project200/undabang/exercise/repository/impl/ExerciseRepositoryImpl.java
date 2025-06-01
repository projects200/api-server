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
import java.util.stream.Collectors;

/**
 * Exercise 엔티티에 대한 QueryDSL 기반 커스텀 레포지토리 구현체입니다.
 * 운동 기록 조회와 관련된 복잡한 쿼리를 처리합니다.
 */
public class ExerciseRepositoryImpl extends QuerydslRepositorySupport implements ExerciseRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * QueryDSL 쿼리 팩토리를 주입받는 생성자입니다.
     */
    public ExerciseRepositoryImpl(JPAQueryFactory queryFactory) {
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

        if (respDto != null) {
            List<PictureDataResponse> urlList = queryFactory
                    .select(Projections.fields(PictureDataResponse.class,
                            picture.id.as("pictureId"),
                            picture.pictureUrl,
                            picture.pictureName,
                            picture.pictureExtension))
                    .from(exercisePicture)
                    .join(exercisePicture.picture, picture)
                    .where(exercisePicture.exercise.id.eq(recordId)
                            .and(picture.pictureDeletedAt.isNull()))
                    .fetch();

            respDto.setPictureDataList(urlList.isEmpty() ? Optional.empty() : Optional.of(urlList));
        }

        return respDto;
    }

    /**
     * 특정 회원의 특정 날짜에 해당하는 운동 기록을 조회합니다.
     *
     * <p>이 메서드는 다음과 같은 단계로 동작합니다:</p>
     * <ol>
     *   <li>주어진 날짜의 00:00:00부터 다음 날 00:00:00 직전까지의 시간 범위를 설정</li>
     *   <li>해당 시간 범위 내에 시작된 운동 기록 데이터를 조회</li>
     *   <li>조회된 운동 ID에 해당하는 사진 URL 정보를 별도 쿼리로 조회</li>
     *   <li>두 결과를 결합하여 최종 응답 DTO 생성</li>
     * </ol>
     *
     * <p>사진이 없는 운동 기록의 경우 빈 리스트가 포함됩니다.</p>
     * <p>조회 결과는 운동 시작 시간 기준으로 오름차순 정렬됩니다.</p>
     *
     * @param memberId 조회할 회원의 고유 식별자 UUID
     * @param date 조회할 날짜 (해당 날짜의 모든 운동 기록이 반환됨)
     * @return 해당 날짜의 운동 기록과 관련 사진 URL을 포함한 응답 DTO 리스트.
     *         조회 결과가 없을 경우 빈 리스트 반환.
     */
    @Override
    public List<FindExerciseRecordDateResponseDto> findExerciseRecordByDate(UUID memberId, LocalDate date) {
        QExercise exercise = QExercise.exercise;
        QExercisePicture exercisePicture = QExercisePicture.exercisePicture;
        QPicture picture = QPicture.picture;

        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = startDate.plusDays(1);

        // 특정 날짜의 멤버id 기준으로 운동정보 조회
        List<Tuple> exercises = queryFactory
                .select(exercise.id,
                        exercise.exerciseTitle,
                        exercise.exercisePersonalType,
                        exercise.exerciseStartedAt,
                        exercise.exerciseEndedAt)
                .from(exercise)
                .where(
                        exercise.member.memberId.eq(memberId),
                        exercise.exerciseStartedAt.goe(startDate).and(exercise.exerciseStartedAt.lt(endDate)),
                        exercise.exerciseDeletedAt.isNull()
                )
                .orderBy(exercise.exerciseStartedAt.asc())
                .fetch();

        if(exercises.isEmpty()){
            return Collections.emptyList();
        }

        List<Long> exerciseIds = exercises.stream()
                .map(tuple -> tuple.get(exercise.id))
                .collect(Collectors.toList());

        // 조회된 운동 ID 들에 해당하는 사진 URL들을 조회하여 MAP으로 그룹화
        Map<Long, List<String>> picturesMap = Collections.emptyMap();
        if (!exerciseIds.isEmpty()) {
            picturesMap = queryFactory
                    .select(exercisePicture.exercise.id, picture.pictureUrl)
                    .from(exercisePicture)
                    .join(exercisePicture.picture, picture)
                    .where(exercisePicture.exercise.id.in(exerciseIds)
                            .and(picture.pictureDeletedAt.isNull()))
                    .orderBy(picture.id.asc()) // 사진 ID 순으로 정렬 (선택 사항)
                    .stream()
                    .collect(Collectors.groupingBy(
                            t -> t.get(exercisePicture.exercise.id),
                            Collectors.mapping(t -> t.get(picture.pictureUrl), Collectors.toList())
                    ));
        }

        // 운동정보와 사진 url 목록을 조합하여 DTO 생성
        List<FindExerciseRecordDateResponseDto> responseDtoList = new ArrayList<>();
        for (Tuple exTuple : exercises) {
            Long currentExerciseId = exTuple.get(exercise.id);
            List<String> urlList = picturesMap.getOrDefault(currentExerciseId, Collections.emptyList());

            responseDtoList.add(new FindExerciseRecordDateResponseDto(
                    currentExerciseId,
                    exTuple.get(exercise.exerciseTitle),
                    exTuple.get(exercise.exercisePersonalType),
                    exTuple.get(exercise.exerciseStartedAt),
                    exTuple.get(exercise.exerciseEndedAt),
                    urlList
            ));
        }

        return responseDtoList;
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

            if (dateMap.containsKey(localDate)) {
                responseDtoList.add(dto);
            } else {
                responseDtoList.add(new FindExerciseRecordByPeriodResponseDto(localDate, 0L));
            }
        }


        return responseDtoList;
    }
}
