package com.project200.undabang.exercise.service;

import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UpdateExerciseService {
    ExerciseIdResponseDto updateExerciseImages(Long exerciseId, UpdateExerciseRequestDto requestDto);
    void checkStartEndDate(LocalDateTime startDate, LocalDateTime endDate);
    void checkMemberExerciseId(UUID memberId, Long exerciseId, List<Long> pictureList);
}
