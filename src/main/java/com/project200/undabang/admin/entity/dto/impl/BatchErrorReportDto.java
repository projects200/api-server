package com.project200.undabang.admin.entity.dto.impl;

import com.project200.undabang.admin.entity.dto.error.CommonErrorData;
import com.project200.undabang.admin.entity.dto.ErrorReportDto;
import com.project200.undabang.admin.entity.dto.error.BatchErrorData;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchErrorReportDto implements ErrorReportDto<BatchErrorData> {
    private final CommonErrorData commonErrorData;
    private final BatchErrorData batchErrorData;

    @Override
    public CommonErrorData getCommonErrorData() {
        return commonErrorData;
    }

    @Override
    public BatchErrorData getSpecificData() {
        return batchErrorData;
    }

    @Override
    public String formattingMessage() {
        StringBuilder sb = new StringBuilder();

        return sb.toString();
    }
}
