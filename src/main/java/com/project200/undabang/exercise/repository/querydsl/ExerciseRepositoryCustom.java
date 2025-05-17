package com.project200.undabang.exercise.repository.querydsl;

import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;

import java.util.UUID;

public interface ExerciseRepositoryCustom {
    boolean existsByRecordIdAndMemberId(UUID memberId, Long recordId);
    FindExerciseRecordResponseDto findExerciseByExerciseId(UUID memberId, Long recordId);
}
