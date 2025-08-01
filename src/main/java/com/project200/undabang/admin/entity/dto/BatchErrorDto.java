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
    public String formattingMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("*!!!!!배치 처리 오류 발생!!!!!*\n");
        sb.append("------------------------------------------\n");
        sb.append("*Job 이름* : ").append(this.getJobName()).append("\n");
        sb.append("*상태* : ").append(this.getStatus()).append("\n");
        sb.append("*Job 파라미터* : ").append(jobParametersToString(this.getJobParameters())).append("\n");
        sb.append("------------------------------------------\n");
        sb.append("*오류 요약*: ").append(super.getSummary()).append("\n");
        sb.append("*에러가 발생한 서비스*: ").append(super.getServiceName()).append("\n");
        sb.append("*에러가 발생한 클래스*: ").append(super.getClassName()).append("\n");
        sb.append("*에러의 심각도*: ").append(super.getErrorLevel()).append("\n");
        sb.append("*에러 발생 시간*: ").append(super.getErrorOccurredAt()).append("\n");
        sb.append("*에러 발생 지점*: ").append(super.getStackTrace()).append("\n");
        sb.append("*에러 발생 환경*: ").append(super.getEnvironment()).append("\n");
        sb.append("*개발자 행동 추천 가이드*: ").append(super.getActionGuide()).append("\n");

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
