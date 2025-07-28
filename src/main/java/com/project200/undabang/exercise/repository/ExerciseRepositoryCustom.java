package com.project200.undabang.exercise.repository;

import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.member.entity.Member;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExerciseRepositoryCustom {
    boolean existsByRecordIdAndMemberId(UUID memberId, Long recordId);
    FindExerciseRecordResponseDto findExerciseByExerciseId(UUID memberId, Long recordId);
    List<FindExerciseRecordDateResponseDto> findExerciseRecordByDate(UUID memberId, LocalDate date);
    List<FindExerciseRecordByPeriodResponseDto> findExercisesByPeriod(UUID memberId, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 회원이 특정 날짜(시간 제외)에 생성한 운동 기록의 수를 조회합니다.
     *
     * @param member 회원 엔티티
     * @param date   조회할 날짜
     * @return 해당 날짜의 운동 기록 수
     */
    long countByMemberAndExerciseStartedAt(Member member, LocalDate date);

    /**
     * 특정 기간 동안 회원의 운동 기록 수를 날짜별로 그룹화하여 조회합니다.
     *
     * @param member    회원 엔티티
     * @param startDate 조회 시작일
     * @param endDate   조회 종료일
     * @return 날짜(key)와 해당 날짜의 운동 기록 수(value)를 담은 Map
     */
    Map<LocalDate, Long> findExerciseCountsByDateBetween(Member member, LocalDate startDate, LocalDate endDate);
}
