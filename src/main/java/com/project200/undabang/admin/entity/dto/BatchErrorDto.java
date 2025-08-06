package com.project200.undabang.admin.entity.dto;

import com.project200.undabang.admin.util.ErrorLogsUtils;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@SuperBuilder
public class BatchErrorDto extends CommonErrorDto {
    private String jobName;
    private JobParameters jobParameters;
    private String status;

    @Override
    public String getTitle() {
        return "*!!!!!배치 처리 오류 발생!!!!!*\n";
    }

    @Override
    public String getSpecificDetails() {
        StringBuilder sb = new StringBuilder();

        sb.append("*Job 이름* : ").append(this.getJobName()).append("\n");
        sb.append("*상태* : ").append(this.getStatus()).append("\n");
        sb.append("*Job 파라미터* : ").append(jobParametersToString(this.getJobParameters())).append("\n");

        return sb.toString();
    }

    private String jobParametersToString(JobParameters jobParameters) {
        if (jobParameters == null || jobParameters.isEmpty()){
            return "no parameters arrived";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, JobParameter<?>> entry : jobParameters.getParameters().entrySet()) {
            sb.append(entry.getKey()).append(" : ");
            sb.append(entry.getValue().getValue()).append("\n");
        }

        return sb.toString();
    }

    public static BatchErrorDto of(Throwable throwable, String serviceName, ErrorLevel errorLevel, String summary, String environment, JobExecution jobExecution) {
        String errorClassName = ErrorLogsUtils.findClassErrorHappened(throwable);
        String actionGuide = ErrorLogsUtils.createActionGuide(throwable);
        String stackTrace = ErrorLogsUtils.getStructuredStackTrace(throwable);

        return BatchErrorDto.builder()
                .serviceName(serviceName)
                .className(errorClassName)
                .errorLevel(errorLevel)
                .summary(summary)
                .errorOccurredAt(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
                .stackTrace(stackTrace)
                .environment(environment)
                .actionGuide(actionGuide)
                .jobName(jobExecution.getJobInstance().getJobName())
                .jobParameters(jobExecution.getJobParameters())
                .status(jobExecution.getStatus().toString())
                .build();
    }
}
