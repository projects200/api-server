package com.project200.undabang.admin.entity.dto.impl;

import com.project200.undabang.admin.entity.dto.error.CommonErrorData;
import com.project200.undabang.admin.entity.dto.ErrorReportDto;
import com.project200.undabang.admin.entity.dto.error.AddScoreErrorData;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebRequestErrorReportDto implements ErrorReportDto<AddScoreErrorData> {
    private final CommonErrorData commonErrorData;
    private final AddScoreErrorData addScoreErrorData;

    @Override
    public CommonErrorData getCommonErrorData() {
        return commonErrorData;
    }

    @Override
    public AddScoreErrorData getSpecificData() {
        return addScoreErrorData;
    }

    @Override
    public String formattingMessage() {
        StringBuilder sb = new StringBuilder();

        return sb.toString();
    }
}
