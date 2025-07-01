package com.project200.undabang.common.batch.listener.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DecreaseExerciseScoreJobListener implements JobExecutionListener {
    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(">>>> {} Job 시작 <<<<", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long durationInMills = jobExecution.getEndTime().compareTo(jobExecution.getStartTime());

        log.info(">>>> {} Job 종료. 상태 : {}, 소요시간 : {} <<<< ", jobExecution.getJobInstance().getJobName(), jobExecution.getStatus(), durationInMills);
    }
}
