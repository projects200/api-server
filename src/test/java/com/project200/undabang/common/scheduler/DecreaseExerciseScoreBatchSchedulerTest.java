package com.project200.undabang.common.scheduler;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@SpringBootTest
class DecreaseExerciseScoreBatchSchedulerTest {

    // jobLauncher를 가짜 객체로 생성해 등록
    // MockBean이 Depreciated 되므로 이렇게 직접 주입
    @TestConfiguration
    static class TestJobLauncherConfig{
        @Bean
        @Primary
        public JobLauncher testJobLauncher(){
            return Mockito.mock(JobLauncher.class);
        }
    }

    @Autowired
    private DecreaseExerciseScoreBatchScheduler scheduler;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("decreaseExerciseScoreJob")
    private Job decreaseExerciseScoreJob;

    @Test
    @DisplayName("스케줄러가 실행되면 JobLauncher 의 run 메소드를 올바른 파라미터와 함께 호출해야 함")
    void runDecreaseExerciseScoreJob() {
        // given && when
        scheduler.runDecreaseExerciseScoreJob();

        // then

        // Awaitility : 비동기 작업이 끝날 때 까지 기다려주는 역할 수행
        // 현재는 5초간 기다리도록 설정
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            // 특정 인자가 넘어가는지, 정확하게 넘어가는지 확인
            // 객체의 상호작용을 확인할 수 있다.
            ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
            ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);

            // job launcher의 run 메소드 감시
            Mockito.verify(jobLauncher).run(jobCaptor.capture(), jobParametersCaptor.capture());

            Job capturedJob = jobCaptor.getValue();
            Assertions.assertThat(capturedJob).isEqualTo(decreaseExerciseScoreJob);

            JobParameters capturedParams = jobParametersCaptor.getValue();
            String expectedRunDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Assertions.assertThat(capturedParams.getString("runDate")).isEqualTo(expectedRunDate);
        });
    }
}