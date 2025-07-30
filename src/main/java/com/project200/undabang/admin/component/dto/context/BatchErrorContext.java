package com.project200.undabang.admin.component.dto.context;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchErrorContext {
    private String jobName;
    private String jobParameters;
    private String status;
}
