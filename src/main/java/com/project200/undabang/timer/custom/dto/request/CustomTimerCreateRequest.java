package com.project200.undabang.timer.custom.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomTimerCreateRequest {
    @NotBlank
    @Size(min = 1, max = 100, message = "커스텀 타이머 이름은 최소 1글자, 최대 100글자 이내로 작성하여 주시길 바랍니다.")
    private String customTimerName;

    @Valid
    @NotNull
    private List<CustomTimerStepCreateRequest> customTimerSteps;
}
