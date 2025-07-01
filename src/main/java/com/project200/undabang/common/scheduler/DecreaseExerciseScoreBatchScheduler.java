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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * '운동 점수 감소' 배치 잡DecreaseExerciseScoreJobConfig을 주기적으로 실행하는 스케줄러 클래스입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DecreaseExerciseScoreBatchScheduler {
    private final JobLauncher jobLauncher;

    @Qualifier("decreaseExerciseScoreJob")
    private final Job decreaseExerciseScoreJob;

    /**
     * 매일 새벽 3시에 '운동 점수 감소' 배치 잡을 비동기적으로 실행합니다.
     * cron 표현식 "0 0 3 * * ?"는 매일 3시 0분 0초에 작업을 실행하도록 설정합니다.
     * Job 실행 시 'runDate'라는 JobParameter에 현재 시각을 yyyy-MM-dd'T'HH:mm:ss 형식의 문자열로 전달합니다.
     * Job 실행 중 예외가 발생하면 에러 로그를 기록합니다.
     * 또한, 배치 작업이 매우 길어질 수 있으므로, @Async annotation을 사용하여 스케줄러 쓰레드를 너무 오래 점유하지 않도록 합니다.
     * (추후 슬랙 알림 등의 모니터링 기능 추가 예정)
     */
    @Async("decreaseExerciseScoreBatchJobExecutor")
    @Scheduled(cron = "0 0 3 * * ?")
    public void runDecreaseExerciseScoreJob() {
        log.info("운동 점수 감소 배치 Job Scheduler 진행");

        try{
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("runDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .toJobParameters();

            jobLauncher.run(decreaseExerciseScoreJob, jobParameters);
        }catch (Exception e){
            log.error("운동 점수 감소 배치 Job 실행 중 오류 발생", e);
            // 요기에 슬랙 API 전송 하도록 설정하면 될듯
        }
    }
}
