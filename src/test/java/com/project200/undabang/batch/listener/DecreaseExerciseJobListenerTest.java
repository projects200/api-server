package com.project200.undabang.batch.listener;

import com.project200.undabang.admin.component.NotifyErrorToAdmin;
import com.project200.undabang.admin.entity.dto.BatchErrorDto;
import com.project200.undabang.admin.entity.dto.ErrorLevel;
import com.project200.undabang.admin.util.ErrorLogsUtils;
import com.project200.undabang.common.batch.listener.job.DecreaseExerciseScoreJobListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DecreaseExerciseJobListenerTest {
    @InjectMocks
    private DecreaseExerciseScoreJobListener jobListener;

    @Mock
    private JobExecution jobExecution;

    @Mock
    private JobInstance jobInstance;

    @Mock
    private NotifyErrorToAdmin notifyErrorToAdmin;

    @Nested
    @DisplayName("afterJob 메소드는")
    class test_afterJob{
        private void setUpJobExecution(){
            String jobName = "decreaseExerciseScoreJob";

            when(jobExecution.getJobInstance()).thenReturn(jobInstance);
            when(jobInstance.getJobName()).thenReturn(jobName);
            when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
            when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusSeconds(5));
        }

        @Test
        @DisplayName("Job 이 성공하면 알림을 보내지 않는다")
        void afterJob_logsJobStatusAndName(){
            // given
            setUpJobExecution();
            when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

            // when
            jobListener.afterJob(jobExecution);

            // then
            verify(notifyErrorToAdmin, never()).sendErrorToSlackApi(any());
        }

        @Test
        @DisplayName("Job이 실패하면, 슬랙으로 에러 알림을 보낸다")
        void afterJob_whenJobFailed_logsJobStatusAndName() {
            // given
            String profile = "test";
            RuntimeException exception = new RuntimeException("test exception");
            JobParameters jobParameters = new JobParameters();
            setUpJobExecution();

            when(jobExecution.getAllFailureExceptions()).thenReturn(Collections.singletonList(exception));
            when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED); // Job 상태를 FAILED로 설정
            when(jobExecution.getJobParameters()).thenReturn(jobParameters);

            ReflectionTestUtils.setField(jobListener, "profile", profile);

            try(MockedStatic<ErrorLogsUtils> ignoredUtil = mockStatic(ErrorLogsUtils.class)){
                ignoredUtil.when(() -> ErrorLogsUtils.findClassErrorHappened(exception)).thenReturn("com.example.FakeErrorClass");
                ignoredUtil.when(() -> ErrorLogsUtils.getStructuredStackTrace(exception)).thenReturn("stacktrace...");


                // when
                jobListener.afterJob(jobExecution);

                // then
                ArgumentCaptor<BatchErrorDto> dtoCaptor = ArgumentCaptor.forClass(BatchErrorDto.class);

                verify(notifyErrorToAdmin).sendErrorToSlackApi(dtoCaptor.capture());

                BatchErrorDto capturedDto = dtoCaptor.getValue();

                assertThat(capturedDto.getJobName()).isEqualTo("decreaseExerciseScoreJob");
                assertThat(capturedDto.getStatus()).isEqualTo(BatchStatus.FAILED.toString());
                assertThat(capturedDto.getEnvironment()).isEqualTo(profile);
                assertThat(capturedDto.getErrorLevel()).isEqualTo(ErrorLevel.ERROR);
                assertThat(capturedDto.getSummary()).isEqualTo("배치 작업 실패 알림 입니다.");
                assertThat(capturedDto.getClassName()).isEqualTo("com.example.FakeErrorClass");
                assertThat(capturedDto.getStackTrace()).isEqualTo("stacktrace...");
                assertThat(capturedDto.getJobParameters()).isEqualTo(jobParameters);
            }
        }
    }
}
