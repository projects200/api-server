package com.project200.undabang.common.batch.listener.job;

import com.project200.undabang.admin.component.NotifyErrorToAdmin;
import com.project200.undabang.admin.component.dto.CommonErrorData;
import com.project200.undabang.admin.component.dto.ErrorLevel;
import com.project200.undabang.admin.component.dto.context.BatchErrorContext;
import com.project200.undabang.admin.component.dto.impl.BatchErrorReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecreaseExerciseScoreJobListener implements JobExecutionListener {
    private final NotifyErrorToAdmin notifyErrorToAdmin;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(">>>> {} Job 시작 <<<<", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long durationInMills = java.time.Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
        String jobName = jobExecution.getJobInstance().getJobName();

        if(jobExecution.getStatus() == BatchStatus.FAILED){
            log.error(">>>> {} Job 실패. 상태 : {}, 소요시간 : {} <<<< ", jobName, jobExecution.getStatus(), durationInMills);

            BatchErrorContext batchErrorContext = BatchErrorContext.builder()
                    .jobName(jobName)
                    .jobParameters(jobExecution.getJobParameters().toString())
                    .status(jobExecution.getStatus().toString())
                    .build();

            CommonErrorData commonErrorData = CommonErrorData.builder()
                    .serviceName("DecreaseExerciseScoreJob")
                    .errorLevel(ErrorLevel.ERROR)
                    .summary(String.format("배치 작업 [%s] 실패", batchErrorContext.getJobName()))
                    .errorOccurredAt(LocalDateTime.now())
                    .stackTrace("에러를 찾는 함수(getStackTraceAsString)에서 에러를 찾아서 반환할 예정")
                    .build();

            BatchErrorReportDto reportDto = BatchErrorReportDto.builder()
                    .batchErrorContext(batchErrorContext)
                    .commonErrorData(commonErrorData)
                    .build();

            notifyErrorToAdmin.sendErrorNotification(reportDto);
        }else{
            log.info(">>>> {} Job 종료. 상태 : {}, 소요시간 : {} <<<< ", jobName, jobExecution.getStatus(), durationInMills);
        }
    }

    private String getStackTraceAsString(JobExecution jobExecution){
        // 에러 메시지 만드는 헬퍼 함수
        // getRootCause 사용
        return null;
    }

    private Throwable getRootCause(JobExecution jobExecution){
        // 에러 원인을 계속 파고 들어가는 함수 (GlobalExceptionHandler 참조)
        // 가장 기본 원인만 반환?
        // 전부 반환?
        return null;
    }


}
