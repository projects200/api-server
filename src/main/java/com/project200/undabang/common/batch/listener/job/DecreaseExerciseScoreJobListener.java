package com.project200.undabang.common.batch.listener.job;

import com.project200.undabang.common.notification.command.NotificationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecreaseExerciseScoreJobListener implements JobExecutionListener {
    private final NotificationCommand notificationCommand;

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
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append(">>>> 배치 잡 실행 실패!! <<<<\n")
                    .append("- Job : ").append(jobName).append("\n")
                    .append("- Status: ").append(jobExecution.getStatus()).append("\n")
                    .append("- JobParameters: ").append(jobExecution.getJobParameters());

            notificationCommand.sendErrorNotification(errorMessage.toString());
        }else{
            log.info(">>>> {} Job 종료. 상태 : {}, 소요시간 : {} <<<< ", jobName, jobExecution.getStatus(), durationInMills);
        }
    }
}
