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
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
class DecreaseExerciseScoreBatchSchedulerTest {

    // jobLauncher를 가짜 객체로 생성해 등록
    @MockitoBean
    private JobLauncher jobLauncher;

    @Autowired
    private DecreaseExerciseScoreBatchScheduler scheduler;

    @Autowired
    @Qualifier("decreaseExerciseScoreJob")
    private Job decreaseExerciseScoreJob;

    @Test
    @DisplayName("스케줄러가 실행되면 올바른 Job과 오늘 날짜가 포함된 타임스탬프 형식의 JobParameter를 사용하여 JobLauncher를 호출해야 함")
    void runDecreaseExerciseScoreJob() throws Exception {
        // given && when
        scheduler.runDecreaseExerciseScoreJob();

        // then
        // 비동기(@Async) 작업이 완료될 때까지 최대 5초간 대기
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            // Job과 JobParameters 인자를 캡처하기 위한 ArgumentCaptor 생성
            ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
            ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);

            // jobLauncher.run 메서드가 호출되었는지 검증하고, 인자를 캡처
            Mockito.verify(jobLauncher).run(jobCaptor.capture(), jobParametersCaptor.capture());

            // 1. Job 검증: 올바른 Job Bean이 전달되었는지 확인
            Job capturedJob = jobCaptor.getValue();
            assertThat(capturedJob).isEqualTo(decreaseExerciseScoreJob);

            // 2. JobParameters 검증: [핵심 변경 사항]
            JobParameters capturedParams = jobParametersCaptor.getValue();
            String runDateString = capturedParams.getString("runDate");

            // runDate 파라미터가 null이 아니고, 비어있지 않은지 먼저 확인
            assertThat(runDateString).isNotNull().isNotEmpty();

            // 캡처된 타임스탬프 문자열을 LocalDateTime으로 파싱
            LocalDateTime capturedDateTime = LocalDateTime.parse(runDateString);
            // 파싱된 객체에서 날짜 부분만 추출
            LocalDate capturedDate = capturedDateTime.toLocalDate();

            // 추출된 날짜가 오늘 날짜와 같은지 비교
            assertThat(capturedDate).isEqualTo(LocalDate.now());
        });
    }
}