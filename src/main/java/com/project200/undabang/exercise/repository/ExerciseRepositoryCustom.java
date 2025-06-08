package com.project200.undabang.exercise.repository;

import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExerciseRepositoryCustom {
    boolean existsByRecordIdAndMemberId(UUID memberId, Long recordId);
    FindExerciseRecordResponseDto findExerciseByExerciseId(UUID memberId, Long recordId);
    List<FindExerciseRecordDateResponseDto> findExerciseRecordByDate(UUID memberId, LocalDate date);
    List<FindExerciseRecordByPeriodResponseDto> findExercisesByPeriod(UUID memberId, LocalDate startDate, LocalDate endDate);
}
