package com.project200.undabang.exercise.dto.request;

import com.project200.undabang.common.validation.StartBeforeEnd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@StartBeforeEnd(startTimeFieldName = "exerciseStartedAt", endTimeFieldName = "exerciseEndedAt")
public class UpdateExerciseRequestDto {
    @Size(max = 255)
    @NotBlank(message = "제목은 필수 입력값입니다.")
    private String exerciseTitle;

    @Size(max = 255)
    private String exercisePersonalType;

    @Size(max = 255)
    private String exerciseLocation;
    private String exerciseDetail;

    @NotNull(message = "시작 일시는 필수 입력값입니다.")
    @Past(message = "시작 일시는 현재 시간 이전이어야 합니다.")
    private LocalDateTime exerciseStartedAt;

    @NotNull(message = "종료 일시는 필수 입력값입니다.")
    @Past(message = "종료 일시는 현재 시간 이전이어야 합니다.")
    private LocalDateTime exerciseEndedAt;
}