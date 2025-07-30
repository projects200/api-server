package com.project200.undabang.admin.component.dto.impl;

import com.project200.undabang.admin.component.dto.CommonErrorData;
import com.project200.undabang.admin.component.dto.ErrorReport;
import com.project200.undabang.admin.component.dto.context.BatchErrorContext;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchErrorReportDto implements ErrorReport<BatchErrorContext> {
    private final CommonErrorData commonErrorData;
    private final BatchErrorContext batchErrorContext;

    @Override
    public CommonErrorData getCommonErrorData() {
        return commonErrorData;
    }

    @Override
    public BatchErrorContext getContextData() {
        return batchErrorContext;
    }

    @Override
    public String formattingMessage() {
        StringBuilder sb = new StringBuilder();

        return sb.toString();
    }
}
