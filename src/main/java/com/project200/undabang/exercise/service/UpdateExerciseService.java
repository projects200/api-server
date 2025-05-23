package com.project200.undabang.exercise.service;

import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UpdateExerciseService {
    CreateExerciseResponseDto updateExerciseImages(UpdateExerciseRequestDto requestDto);
    void checkStartEndDate(LocalDateTime startDate, LocalDateTime endDate);
    void checkMemberExerciseId(UUID memberId, Long exerciseId, List<Long> pictureList);
}
