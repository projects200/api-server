package com.project200.undabang.batch.listener.step;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DecreaseExerciseScoreStepListener implements StepExecutionListener {
    private final ThreadLocal<Long> stepStartTime = new ThreadLocal<>();

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepStartTime.set(System.currentTimeMillis());
        log.info(">>>>>> {} Step 시작", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long duration = System.currentTimeMillis() - stepStartTime.get();
        stepStartTime.remove();

        log.info(">>>>>> {} Step 종료. 상태 : {}, 소요시간 : {}", stepExecution.getStepName(), stepExecution.getStatus(), duration);

        return stepExecution.getExitStatus();
    }
}
