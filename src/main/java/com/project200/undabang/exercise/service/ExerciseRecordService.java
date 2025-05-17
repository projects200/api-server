package com.project200.undabang.exercise.service;

import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;

import java.util.UUID;

public interface ExerciseRecordService {
    FindExerciseRecordResponseDto findExerciseRecordByRecordId(Long recordId);
    boolean checkMemberId(UUID memberId);
    boolean checkExerciseRecordId(Long recordId);
    boolean validateMemberRecordAccess(UUID memberId, Long recordId);
}
