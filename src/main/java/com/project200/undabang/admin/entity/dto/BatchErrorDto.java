package com.project200.undabang.admin.entity.dto;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

import java.util.Map;

@Getter
@SuperBuilder
public class BatchErrorDto extends CommonErrorDto{
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

    private String jobParametersToString(JobParameters jobParameters){
        if(jobParameters == null || jobParameters.isEmpty()){
            return "no parameters arrived";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, JobParameter<?>> entry : jobParameters.getParameters().entrySet()) {
            sb.append(entry.getKey()).append(" : ");
            sb.append(entry.getValue().getValue()).append("\n");
        }

        return sb.toString();
    }
}
