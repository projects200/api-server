package com.project200.undabang.batch.listener;

import com.project200.undabang.batch.listener.job.DecreaseExerciseScoreJobListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DecreaseExerciseJobListenerTest {
    @InjectMocks
    private DecreaseExerciseScoreJobListener jobListener;

    @Mock
    private JobExecution jobExecution;

    @Mock
    private JobInstance jobInstance;

    @Test
    @DisplayName("afterJob 메소드가 호출되면 Job의 이름, 상태, 소요시간에 대한 로그를 남긴다")
    void afterJob_logsJobStatusAndName(){
        // given
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("decreaseExerciseScoreJob");
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusSeconds(5));

        // when
        jobListener.afterJob(jobExecution);

        // then
        verify(jobExecution, times(1)).getJobInstance();
        verify(jobExecution, times(1)).getStatus();
        verify(jobExecution, times(1)).getEndTime();
        verify(jobExecution, times(1)).getStartTime();
    }

    @Test
    @DisplayName("Job이 실패했을 때 afterJob 메소드가 호출되면 Job의 이름, 상태, 소요시간에 대한 로그를 남긴다")
    void afterJob_whenJobFailed_logsJobStatusAndName() {
        // given
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("decreaseExerciseScoreJob");
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED); // Job 상태를 FAILED로 설정
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusSeconds(5));

        // when
        jobListener.afterJob(jobExecution);

        // then
        // Job 실패 시에도 관련 정보를 가져와 로그를 남기는지 검증
        verify(jobExecution, times(1)).getJobInstance();
        verify(jobExecution, times(1)).getStatus();
        verify(jobExecution, times(1)).getEndTime();
        verify(jobExecution, times(1)).getStartTime();
    }
}
