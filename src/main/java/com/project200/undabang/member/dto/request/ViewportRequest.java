package com.project200.undabang.member.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ViewportRequest {

    @NotNull(message = "좌상단 위도는 필수입니다.")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double leftTopLatitude;

    @NotNull(message = "좌상단 경도는 필수입니다.")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double leftTopLongitude;

    @NotNull(message = "우하단 위도는 필수입니다.")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double rightBottomLatitude;

    @NotNull(message = "우하단 경도는 필수입니다.")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double rightBottomLongitude;
}
