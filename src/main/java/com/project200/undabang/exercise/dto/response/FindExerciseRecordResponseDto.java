package com.project200.undabang.exercise.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindExerciseRecordResponseDto {
    private String exerciseTitle;
    private String exerciseDetail;
    private String exercisePersonalType;
    private LocalDateTime exerciseStartedAt;
    private LocalDateTime exerciseEndedAt;
    private String memberLocationTitle;
    private Optional<List<String>> exercisePictureUrls;
}
