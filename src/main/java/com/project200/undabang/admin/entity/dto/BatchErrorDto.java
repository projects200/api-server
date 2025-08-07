package com.project200.undabang.admin.entity.dto;

import lombok.Getter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

import java.util.Map;

@Getter
public class BatchErrorDto extends CommonErrorDto {
    private String jobName;
    private JobParameters jobParameters;
    private String status;

    private BatchErrorDto(Throwable throwable, String serviceName, ErrorLevel errorLevel, String summary, String environment, String jobName, JobParameters jobParameters, String status) {
        super(throwable, serviceName, errorLevel, summary, environment);
        this.jobName = jobName;
        this.jobParameters = jobParameters;
        this.status = status;
    }


    @Override
    public String getTitle() {
        return "*배치 처리 오류 발생*\n";
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
        String jobName = jobExecution.getJobInstance().getJobName();
        JobParameters jobParameters = jobExecution.getJobParameters();
        String status = jobExecution.getStatus().toString();

        return new BatchErrorDto(throwable, serviceName, errorLevel, summary, environment, jobName, jobParameters, status);
    }
}
