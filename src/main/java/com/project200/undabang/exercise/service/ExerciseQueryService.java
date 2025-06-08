package com.project200.undabang.exercise.service;

import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExerciseQueryService {
    FindExerciseRecordResponseDto findExerciseRecordByRecordId(Long recordId);
    List<FindExerciseRecordDateResponseDto> findExerciseRecordByDate(LocalDate inputDate);
    List<FindExerciseRecordByPeriodResponseDto> findExerciseRecordsByPeriod(LocalDate startDate, LocalDate endDate);
    boolean checkMemberId(UUID memberId);
    boolean checkExerciseRecordId(Long recordId);
    boolean validateMemberRecordAccess(UUID memberId, Long recordId);

}
