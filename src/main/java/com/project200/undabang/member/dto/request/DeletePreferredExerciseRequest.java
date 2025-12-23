package com.project200.undabang.member.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class DeletePreferredExerciseRequest {
    @NotNull(message = "삭제할 운동 ID 목록은 필수입니다.")
    @Size(min = 1, max = 5)
    private List<Long> preferredExerciseIds;
}
