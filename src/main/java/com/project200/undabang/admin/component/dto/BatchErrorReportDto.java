package com.project200.undabang.admin.component.dto;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder
public class BatchErrorReportDto extends ErrorReportDto {
    private final String jobName; // 실행된 배치의 Job 이름
    private final Map<String, String> jobParameters; // Job 실행시 사용된 파라미터
}
