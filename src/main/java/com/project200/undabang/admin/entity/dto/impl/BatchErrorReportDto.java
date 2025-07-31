package com.project200.undabang.admin.entity.dto.impl;

import com.project200.undabang.admin.entity.dto.ErrorReportDto;
import com.project200.undabang.admin.entity.dto.error.BatchErrorData;
import com.project200.undabang.admin.entity.dto.error.CommonErrorData;
import lombok.Builder;
import lombok.Getter;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

import java.time.temporal.ChronoUnit;
import java.util.Map;

@Getter
@Builder
public class BatchErrorReportDto implements ErrorReportDto<BatchErrorData> {
    private final CommonErrorData commonErrorData;
    private final BatchErrorData batchErrorData;

    @Override
    public CommonErrorData getCommonErrorData() {
        return commonErrorData;
    }

    @Override
    public BatchErrorData getSpecificData() {
        return batchErrorData;
    }

    @Override
    public String formattingMessage() {
        CommonErrorData commonErrorData = getCommonErrorData();
        BatchErrorData batchErrorData = getSpecificData();

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("*!!!!!배치 처리 오류 발생!!!!!*\n"));
        sb.append("------------------------------------------\n");
        sb.append(String.format("*Job 이름*: `%s`\n", batchErrorData.getJobName()));
        sb.append(String.format("*상태*: `%s`\n", batchErrorData.getStatus()));
        sb.append(String.format("*Job 파라미터*:\n```%s```\n", jobParametersToString(batchErrorData.getJobParameters())));
        sb.append("------------------------------------------\n");
        sb.append("*오류 요약*: ").append(commonErrorData.getSummary()).append("\n");
        sb.append("*에러가 발생한 서비스*: ").append(commonErrorData.getServiceName()).append("\n");
        sb.append("*에러가 발생한 클래스*: ").append(commonErrorData.getClassName()).append("\n");
        sb.append("*에러의 심각도*: ").append(commonErrorData.getErrorLevel()).append("\n");
        sb.append("*에러 발생 시간*: ").append(commonErrorData.getErrorOccurredAt().truncatedTo(ChronoUnit.SECONDS)).append("\n");
        sb.append("*에러 발생 지점*: ").append(commonErrorData.getStackTrace()).append("\n");
        sb.append("*에러 발생 환경*: ").append(commonErrorData.getEnvironment()).append("\n");
        sb.append("*개발자 행동 추천 가이드*: ").append(commonErrorData.getActionGuide()).append("\n");

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
