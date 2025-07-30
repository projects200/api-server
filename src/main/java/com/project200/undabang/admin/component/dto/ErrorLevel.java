package com.project200.undabang.admin.component.dto;

public enum ErrorLevel {
    CRITICAL, // 즉시 확인 필요
    ERROR, // 서비스는 유지되지만 확인이 필요한 경우
    WARN // 지금 당장 문제는 아니지만 잠재적인 위험 신호가 있는 경우
}
