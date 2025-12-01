package com.project200.undabang.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteExpiredFcmTokenBatchScheduler {
    private final JobLauncher jobLauncher;

    @Qualifier("deleteExpiredFcmTokenJob")
    private final Job deleteExpiredFcmTokenJob;

    @Async("decreaseExerciseScoreBatchJobExecutor")
    @Scheduled(cron = "0 0 5 * * SUN")
    public void runDeleteExpiredFcmTokenJob() throws Exception {
        log.info("주간 만료 FCM 토큰 제거 작업 진행");

        String runDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));

        JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDateTime("executedAt", LocalDateTime.now())
                .toJobParameters();

        jobLauncher.run(deleteExpiredFcmTokenJob, jobParameters);
    }
}
