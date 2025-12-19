package com.project200.undabang.member.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class DeletePreferredExerciseRequest {
    @NotEmpty(message = "삭제할 운동 ID 목록은 필수입니다.")
    private List<Long> preferredExerciseIds;
}
