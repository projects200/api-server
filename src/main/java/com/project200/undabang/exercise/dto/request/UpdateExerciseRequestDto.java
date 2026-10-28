package com.project200.undabang.exercise.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.AssertTrue;
import com.project200.undabang.common.validation.StartBeforeEnd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@StartBeforeEnd(startTimeFieldName = "exerciseStartedAt", endTimeFieldName = "exerciseEndedAt")
public class UpdateExerciseRequestDto {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID editId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long baseVersion;

    @JsonIgnore
    @AssertTrue(message = "editId와 0 이상의 baseVersion을 함께 입력해주세요.")
    public boolean isSyncMetadataValid() {
        return (editId == null && baseVersion == null)
                || (editId != null && baseVersion != null && baseVersion >= 0);
    }

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