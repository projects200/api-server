package com.project200.undabang.exercise.service;

import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseRecordService {
    FindExerciseRecordResponseDto findExerciseRecordByRecordId(Long recordId);
    Optional<List<FindExerciseRecordDateResponseDto>> findExerciseRecordByDate(LocalDate inputDate);
    boolean checkMemberId(UUID memberId);
    boolean checkExerciseRecordId(Long recordId);
    boolean validateMemberRecordAccess(UUID memberId, Long recordId);

}
