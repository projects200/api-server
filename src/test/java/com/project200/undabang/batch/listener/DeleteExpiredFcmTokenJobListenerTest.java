package com.project200.undabang.batch.listener;

import com.project200.undabang.admin.component.NotifyErrorToAdmin;
import com.project200.undabang.admin.entity.dto.BatchErrorDto;
import com.project200.undabang.admin.entity.dto.ErrorLevel;
import com.project200.undabang.admin.util.ErrorLogsUtils;
import com.project200.undabang.common.batch.listener.job.DeleteExpiredFcmTokenJobListener;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteExpiredFcmTokenJobListener 단위 테스트")
public class DeleteExpiredFcmTokenJobListenerTest {

    @InjectMocks
    private DeleteExpiredFcmTokenJobListener jobListener;

    @Mock
    private JobExecution jobExecution;

    @Mock
    private JobInstance jobInstance;

    @Mock
    private NotifyErrorToAdmin notifyErrorToAdmin;

    @Nested
    @DisplayName("afterJob 메소드는")
    class test_afterJob {

        private void setUpJobExecution() {
            String jobName = "deleteExpiredFcmTokenJob";

            when(jobExecution.getJobInstance()).thenReturn(jobInstance);
            when(jobInstance.getJobName()).thenReturn(jobName);
            when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
            when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusSeconds(5));
        }

        @Test
        @DisplayName("Job 이 성공하면 알림을 보내지 않는다")
        void afterJob_whenJobCompleted_doesNotSendNotification() {
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
        void afterJob_whenJobFailed_sendsNotificationToSlack() {
            // given
            String profile = "test";
            RuntimeException exception = new RuntimeException("test exception");
            JobParameters jobParameters = new JobParameters();

            setUpJobExecution();

            // 실패 상황 세팅
            when(jobExecution.getAllFailureExceptions()).thenReturn(Collections.singletonList(exception));
            when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
            when(jobExecution.getJobParameters()).thenReturn(jobParameters);

            // 프로필 주입
            ReflectionTestUtils.setField(jobListener, "profile", profile);

            // Static Utils Mocking (이전 테스트 스타일 반영)
            // ErrorLogsUtils가 BatchErrorDto.of 내부에서 쓰인다고 가정
            try (MockedStatic<ErrorLogsUtils> ignoredUtil = mockStatic(ErrorLogsUtils.class)) {
                // 필요시 Stubbing 추가. BatchErrorDto.of() 내부 로직에 따라 필요할 수 있음.
                // 만약 BatchErrorDto.of()가 ErrorLogsUtils를 호출한다면 아래와 같이 Stubbing
                ignoredUtil.when(() -> ErrorLogsUtils.findClassErrorHappened(exception)).thenReturn("com.example.FakeErrorClass");
                ignoredUtil.when(() -> ErrorLogsUtils.getStructuredStackTrace(exception)).thenReturn("stacktrace...");

                // when
                jobListener.afterJob(jobExecution);

                // then
                ArgumentCaptor<BatchErrorDto> dtoCaptor = ArgumentCaptor.forClass(BatchErrorDto.class);
                verify(notifyErrorToAdmin).sendErrorToSlackApi(dtoCaptor.capture());

                BatchErrorDto capturedDto = dtoCaptor.getValue();

                // 검증 (Listener에 하드코딩된 값들과 일치하는지 확인)
                assertThat(capturedDto.getJobName()).isEqualTo("deleteExpiredFcmTokenJob");
                assertThat(capturedDto.getStatus()).isEqualTo(BatchStatus.FAILED.toString());
                assertThat(capturedDto.getEnvironment()).isEqualTo(profile);
                assertThat(capturedDto.getErrorLevel()).isEqualTo(ErrorLevel.ERROR);
                assertThat(capturedDto.getSummary()).isEqualTo("FCM 토큰 삭제 배치 작업 실패 알림 입니다.");
            }
        }
    }
}