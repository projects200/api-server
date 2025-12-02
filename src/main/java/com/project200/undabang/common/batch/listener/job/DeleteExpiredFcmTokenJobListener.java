package com.project200.undabang.common.batch.listener.job;

import com.project200.undabang.admin.component.NotifyErrorToAdmin;
import com.project200.undabang.admin.entity.dto.BatchErrorDto;
import com.project200.undabang.admin.entity.dto.ErrorLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteExpiredFcmTokenJobListener implements JobExecutionListener {
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
            String serviceName = "DeleteExpiredFcmTokenJob";
            String summary = "FCM 토큰 삭제 배치 작업 실패 알림 입니다.";

            BatchErrorDto batchErrorDto = BatchErrorDto.of(throwable, serviceName, ErrorLevel.ERROR, summary, profile, jobExecution);

            notifyErrorToAdmin.sendErrorToSlackApi(batchErrorDto);
        } else {
            log.info(">>>> {} Job 종료. 상태 : {}, 소요시간 : {} <<<< ", jobName, jobExecution.getStatus(), durationInMills);
        }
    }
}
