package com.project200.undabang.admin.component.dto.impl;

import com.project200.undabang.admin.component.dto.CommonErrorData;
import com.project200.undabang.admin.component.dto.ErrorReport;
import com.project200.undabang.admin.component.dto.context.UserErrorContext;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebRequestErrorReportDto implements ErrorReport<UserErrorContext> {
    private final CommonErrorData commonErrorData;
    private final UserErrorContext userErrorContext;

    @Override
    public CommonErrorData getCommonErrorData() {
        return commonErrorData;
    }

    @Override
    public UserErrorContext getContextData() {
        return userErrorContext;
    }

    @Override
    public String formattingMessage() {
        StringBuilder sb = new StringBuilder();

        return sb.toString();
    }
}
