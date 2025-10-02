package com.project200.undabang.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExerciseLocationRequest {
    @NotBlank
    @Size(min = 1, max = 100, message = "운동 장소명은 최대 100글자 입력 가능합니다.")
    private String exerciseLocationName;
}
