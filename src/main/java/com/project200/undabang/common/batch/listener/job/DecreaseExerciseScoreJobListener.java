package com.project200.undabang.common.batch.listener.job;

import com.project200.undabang.admin.component.NotifyErrorToAdmin;
import com.project200.undabang.admin.entity.dto.CommonErrorData;
import com.project200.undabang.admin.entity.dto.ErrorLevel;
import com.project200.undabang.admin.entity.dto.context.BatchErrorData;
import com.project200.undabang.admin.entity.dto.impl.BatchErrorReportDto;
import com.project200.undabang.admin.util.ErrorLogsUtils;
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

        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error(">>>> {} Job 실패. 상태 : {}, 소요시간 : {} <<<< ", jobName, jobExecution.getStatus(), durationInMills);

            Throwable rootCause = jobExecution.getAllFailureExceptions().get(0);

            BatchErrorData batchErrorData = BatchErrorData.builder()
                    .jobName(jobName)
                    .jobParameters(jobExecution.getJobParameters().toString())
                    .status(jobExecution.getStatus().toString())
                    .build();

            CommonErrorData commonErrorData = CommonErrorData.builder()
                    .serviceName("DecreaseExerciseScoreJob")
                    .errorLevel(ErrorLevel.ERROR)
                    .summary(String.format("배치 작업 [%s] 실패", batchErrorData.getJobName()))
                    .errorOccurredAt(LocalDateTime.now())
                    .stackTrace(ErrorLogsUtils.getStructuredStackTrace(rootCause))
                    .build();

            BatchErrorReportDto reportDto = BatchErrorReportDto.builder()
                    .batchErrorData(batchErrorData)
                    .commonErrorData(commonErrorData)
                    .build();

            notifyErrorToAdmin.sendErrorNotification(reportDto);
        } else {
            log.info(">>>> {} Job 종료. 상태 : {}, 소요시간 : {} <<<< ", jobName, jobExecution.getStatus(), durationInMills);
        }
    }
}
