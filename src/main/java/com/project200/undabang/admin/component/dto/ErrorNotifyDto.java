package com.project200.undabang.admin.component.dto;

import lombok.Builder;

@Builder
public class ErrorNotifyDto {
    private String className;
    private String methodName;
    private String message;

}
