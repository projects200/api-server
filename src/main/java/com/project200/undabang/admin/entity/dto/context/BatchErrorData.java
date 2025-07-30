package com.project200.undabang.admin.entity.dto.context;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchErrorData {
    private String jobName;
    private String jobParameters;
    private String status;
}
