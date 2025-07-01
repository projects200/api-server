package com.project200.undabang.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;


/**
 * 애플리케이션의 비동기 처리를 위한 스레드 풀을 설정하는 클래스입니다.
 * Spring의 @Async 어노테이션을 사용하여 작업을 비동기적으로 실행할 때,
 * 별도의 스레드 풀을 설정하지 않으면 기본적으로 내장된 스레드 풀을 사용하게 됩니다.
 * 만약 배치 처리와 같이 시간이 오래 걸리는 작업이 스케줄러에 의해 실행될 경우,
 * 가용한 모든 스레드를 점유하여 다른 비동기 작업이나 스케줄링된 작업이 지연되거나
 * 실행되지 못하는 상황이 발생할 수 있습니다. 최악의 경우, 스레드 고갈로 인해
 * 애플리케이션 전체가 응답 불능 상태에 빠지거나 종료될 수 있습니다.
 * 이러한 문제를 방지하고 애플리케이션의 안정성을 확보하기 위해,
 * 특정 배치 잡을 위한 전용 스레드 풀을 생성하여 작업을 격리합니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * '운동 점수 감소' 배치 잡을 위한 전용 스레드 풀 Executor를 생성하여 빈으로 등록합니다.
     * 이 Executor는 DecreaseExerciseScoreBatchScheduler 에서
     * @Async("decreaseExerciseScoreBatchJobExecutor") 형태로 지정되어 사용됩니다.
     * 이를 통해 해당 배치 잡이 다른 비동기 작업에 영향을 주지 않고 독립적인 스레드에서 실행되도록 보장합니다.
     */
    @Bean(name = "decreaseExerciseScoreBatchJobExecutor")
    public Executor decreaseExerciseScoreBatchJobExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 핵심 쓰레드 수
        executor.setMaxPoolSize(10); // 최대 쓰레드 수
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("decreaseExerciseScoreBatchJobExecutor-"); // 쓰레드 이름 접두사
        executor.initialize();
        return executor;
    }
}
