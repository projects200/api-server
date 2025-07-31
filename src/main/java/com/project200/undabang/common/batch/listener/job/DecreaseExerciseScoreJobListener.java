package com.project200.undabang.common.batch.listener.job;

import com.project200.undabang.admin.component.NotifyErrorToAdmin;
import com.project200.undabang.admin.entity.dto.ErrorLevel;
import com.project200.undabang.admin.entity.dto.error.BatchErrorData;
import com.project200.undabang.admin.entity.dto.error.CommonErrorData;
import com.project200.undabang.admin.entity.dto.impl.BatchErrorReportDto;
import com.project200.undabang.admin.util.ErrorLogsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecreaseExerciseScoreJobListener implements JobExecutionListener {
    private final NotifyErrorToAdmin notifyErrorToAdmin;
    @Value("${spring.profiles.active}")
    private String profile;

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

            Throwable throwable = jobExecution.getAllFailureExceptions().get(0);

            BatchErrorData batchErrorData = BatchErrorData.builder()
                    .jobName(jobName)
                    .jobParameters(jobExecution.getJobParameters())
                    .status(jobExecution.getStatus().toString())
                    .build();

            CommonErrorData commonErrorData = CommonErrorData.builder()
                    .serviceName("DecreaseExerciseScoreJob")
                    .className(ErrorLogsUtils.findClassErrorHappened(throwable))
                    .errorLevel(ErrorLevel.ERROR)
                    .summary(String.format("배치 작업 [%s] 실패 알림입니다.", batchErrorData.getJobName()))
                    .errorOccurredAt(LocalDateTime.now())
                    .stackTrace(ErrorLogsUtils.getStructuredStackTrace(throwable))
                    .environment(profile)
                    .actionGuide("\n 전송되는 로그를 확인 후, 에러를 수정하여 Batch 작업을 다시 돌려주세요!\n")
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
