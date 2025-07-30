package com.project200.undabang.admin.component.dto;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class WebRequestErrorReportDto extends ErrorReportDto{
    private final ErrorUserContext errorUserContext;
}
