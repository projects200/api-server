package com.project200.undabang.common.scheduler;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// 배치 자동 실행 방지
@SpringBootTest(properties = "spring.batch.job.enabled=false")
class DeleteExpiredFcmTokenBatchSchedulerTest {

    // 실제 JobLauncher 대신 Mock Bean 주입
    @MockitoBean
    private JobLauncher jobLauncher;

    @Autowired
    private DeleteExpiredFcmTokenBatchScheduler scheduler;

    // 테스트 대상 Job 주입
    @Autowired
    @Qualifier("deleteExpiredFcmTokenJob")
    private Job deleteExpiredFcmTokenJob;

    @Test
    @DisplayName("스케줄러가 실행되면 올바른 Job과 현재 시간이 포함된 JobParameter를 사용하여 JobLauncher를 호출해야 한다")
    void runDeleteExpiredFcmTokenJob() throws Exception {
        // when: 스케줄러 메서드 실행 (비동기)
        scheduler.runDeleteExpiredFcmTokenJob();

        // then: 비동기 작업 완료 대기 및 검증
        Awaitility.await()
                .atMost(Duration.ofSeconds(5)) // 최대 5초 대기
                .untilAsserted(() -> {
                    // Argument Captor 생성
                    ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
                    ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);

                    // verify: jobLauncher.run() 호출 여부 및 인자 캡처
                    Mockito.verify(jobLauncher).run(jobCaptor.capture(), jobParametersCaptor.capture());

                    // 1. Job 검증: 우리가 기대한 deleteExpiredFcmTokenJob이 맞는지
                    Job capturedJob = jobCaptor.getValue();
                    assertThat(capturedJob).isEqualTo(deleteExpiredFcmTokenJob);

                    // 2. JobParameters 검증
                    JobParameters capturedParams = jobParametersCaptor.getValue();

                    // 스케줄러에서 넣은 "executedAt" 파라미터 확인 (LocalDateTime 타입)
                    LocalDateTime executedAt = capturedParams.getLocalDateTime("executedAt");

                    assertThat(executedAt).isNotNull();
                    // 실행 시간이 현재 시간과 아주 근소한 차이여야 함 (테스트 실행 시점 기준)
                    assertThat(executedAt).isBeforeOrEqualTo(LocalDateTime.now());
                    assertThat(executedAt).isAfter(LocalDateTime.now().minusSeconds(10));
                });
    }
}