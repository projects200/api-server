package com.project200.undabang.timer.custom.dto.request;

import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomTimerStepCreateRequest {
    @NotBlank
    @Size(min = 1, max = 50, message = "커스텀 타이머 스텝 이름은 최소 1글자, 최대 50글자 이내로 작성하여 주시길 바랍니다.")
    private String customTimerStepName;

    @NotNull
    private Byte customTimerStepOrder;

    @NotNull
    @Min(value = 1, message = "0보다 커야 합니다.")
    @Max(value = 3599, message = "1시간 보다 작아야 합니다.")
    private Integer customTimerStepTime;

    /**
     * 이 DTO의 정보를 바탕으로 {@link CustomTimerStep} 엔티티를 생성합니다.
     */
    public CustomTimerStep toEntity(CustomTimer customTimer) {
        return CustomTimerStep.of(
                customTimer,
                this.customTimerStepName,
                this.customTimerStepOrder,
                this.customTimerStepTime
        );
    }
}
