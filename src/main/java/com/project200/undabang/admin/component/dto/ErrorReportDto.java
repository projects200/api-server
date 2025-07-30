package com.project200.undabang.admin.component.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public class ErrorReportDto {
    private final String serviceName;
    private final LocalDateTime errorTime;
    private final String errorMessage;
    private final String exceptionClassName;
    private final String stackTrace;
    private final Object requestDetails; // HTTP 요청 정보 등
    private final String userInfo;
}
