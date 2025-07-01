package com.project200.undabang.batch.listener;

import com.project200.undabang.common.batch.listener.step.DecreaseExerciseScoreStepListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DecreaseExerciseStepListenerTest {
    @InjectMocks
    private DecreaseExerciseScoreStepListener listener;

    @Mock
    private StepExecution stepExecution;

    @Test
    @DisplayName("afterStep 메소드가 호출되면 Step의 상태, 소요시간을 기록함")
    void afterJob_logsStepStatusAndExecutionTime() throws InterruptedException {
        // given
        String stepName = "decreaseExerciseScoreStep";
        ExitStatus expectedExitStatus = ExitStatus.COMPLETED;

        when(stepExecution.getStepName()).thenReturn(stepName);
        when(stepExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(stepExecution.getExitStatus()).thenReturn(expectedExitStatus);

        // when
        listener.beforeStep(stepExecution);
        Thread.sleep(10); // 소요 시간 측정을 위한 약간의 지연
        ExitStatus actualExitStatus = listener.afterStep(stepExecution);

        // then
        assertEquals(expectedExitStatus, actualExitStatus);
    }

    @Test
    @DisplayName("afterStep 메소드가 호출되면 Step의 상태, 소요시간을 기록함")
    void afterJob_whenStepFailed_logsStepStatusAndExecutionTime() throws InterruptedException {
        // given
        String stepName = "decreaseExerciseScoreStep";
        ExitStatus expectedExitStatus = ExitStatus.FAILED;

        when(stepExecution.getStepName()).thenReturn(stepName);
        when(stepExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(stepExecution.getExitStatus()).thenReturn(expectedExitStatus);

        // when
        listener.beforeStep(stepExecution);
        Thread.sleep(10); // 소요 시간 측정을 위한 약간의 지연
        ExitStatus actualExitStatus = listener.afterStep(stepExecution);

        // then
        assertEquals(expectedExitStatus, actualExitStatus);
    }
}
