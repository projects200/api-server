package com.project200.undabang.report.enums;

import lombok.Getter;

@Getter
public enum ReportProcessingStatus {
    PENDING("처리 대기"),
    PROCESSING("처리 중"),
    COMPLETED("처리 완료"),
    REJECTED("거부됨"),
    POSTPONED("보류됨");
    
    private final String description;
    
    ReportProcessingStatus(String description) {
        this.description = description;
    }
}