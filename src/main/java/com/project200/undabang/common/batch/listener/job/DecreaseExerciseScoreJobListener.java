package com.project200.undabang.common.batch.listener.job;

import com.project200.undabang.admin.component.NotifyErrorToAdmin;
import com.project200.undabang.admin.entity.dto.BatchErrorDto;
import com.project200.undabang.admin.entity.dto.ErrorLevel;
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

            BatchErrorDto batchErrorDto = BatchErrorDto.builder()
                    .serviceName("DecreaseExerciseScoreJob")
                    .className(ErrorLogsUtils.findClassErrorHappened(throwable))
                    .errorLevel(ErrorLevel.ERROR)
                    .summary("배치 작업 실패 알림 입니다.")
                    .errorOccurredAt(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
                    .stackTrace(ErrorLogsUtils.getStructuredStackTrace(throwable))
                    .environment(profile)
                    .actionGuide("\n 알림을 통해 제공되는 에러를 확인 후, DB가 종료되었는지 확인해주세요! 만약 DB 연결 오류의 경우 application.yml 파일을 수정해주세요\n")
                    .jobName(jobName)
                    .jobParameters(jobExecution.getJobParameters())
                    .status(jobExecution.getStatus().toString())
                    .build();

            notifyErrorToAdmin.sendBatchErrorToSlack(batchErrorDto);
        } else {
            log.info(">>>> {} Job 종료. 상태 : {}, 소요시간 : {} <<<< ", jobName, jobExecution.getStatus(), durationInMills);
        }
    }
}
