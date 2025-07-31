package com.project200.undabang.admin.entity.dto.error;

import lombok.Builder;
import lombok.Getter;
import org.springframework.batch.core.JobParameters;

@Getter
@Builder
public class BatchErrorData {
    private String jobName; // 에러가 발생한 Job 이름
    private JobParameters jobParameters; // 에러가 발생한 Job에 들어가는 Parameters
    private String status; // 배치 작업 상태
}
