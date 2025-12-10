package com.project200.undabang.common.config;

import com.project200.undabang.common.properties.BatchAsyncProperties;
import com.project200.undabang.common.properties.GeneralAsyncProperties;
import com.project200.undabang.common.properties.SlackAsyncProperties;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AsyncConfig {
    private final BatchAsyncProperties batchAsyncProperties;
    private final SlackAsyncProperties slackAsyncProperties;
    private final GeneralAsyncProperties generalAsyncProperties;

    /**
     * 배치 작업을 위한 전용 스레드 풀 Executor를 생성하여 빈으로 등록합니다.
     * 이 Executor는 배치 스케줄러에서 @Async("batchJobExecutor") 형태로 지정되어 사용됩니다.
     * 이를 통해 배치 작업이 다른 비동기 작업에 영향을 주지 않고 독립적인 스레드에서 실행되도록 보장합니다.
     */
    @Bean(name = "batchJobExecutor")
    public Executor batchJobExecutor() {
        return createThreadPoolTaskExecutor(batchAsyncProperties.getThreadNamePrefix(),
                batchAsyncProperties.getCorePoolSize(),
                batchAsyncProperties.getMaxPoolSize(),
                batchAsyncProperties.getQueueCapacity());
    }

    /**
     * Slack 메시지 알림을 비동기로 전송하기 위해 전용 쓰레드풀을 생성하여 빈으로 등록합니다.
     * 따라서 메시지 전송이 다른 비동기 작업에 영향을 주지 않고 독립된 쓰레드에서 실행되도록 보장합니다.
     */
    @Bean(name = "slackMessageSenderExecutor")
    public Executor slackMessageSenderExecutor() {
        return createThreadPoolTaskExecutor(slackAsyncProperties.getThreadNamePrefix(),
                slackAsyncProperties.getCorePoolSize(),
                slackAsyncProperties.getMaxPoolSize(),
                slackAsyncProperties.getQueueCapacity());
    }

    /**
     * 일반적인 비동기 작업을 처리하기 위한 스레드 풀 Executor를 생성하여 빈으로 등록합니다.
     * 이 Executor는 다양한 비동기 작업에서 재사용할 수 있도록 설계되었습니다.
     */
    @Bean(name = "generalPurposeAsyncExecutor")
    public Executor generalPurposeAsyncExecutor() {
        return createThreadPoolTaskExecutor(generalAsyncProperties.getThreadNamePrefix(),
                generalAsyncProperties.getCorePoolSize(),
                generalAsyncProperties.getMaxPoolSize(),
                generalAsyncProperties.getQueueCapacity());
    }

    /**
     * ThreadPoolTaskExecutor를 생성 및 초기화하는 메서드입니다.
     * 주어진 매개변수를 기반으로 스레드 풀의 구성 요소를 설정합니다.
     *
     * @param threadNamePrefix 생성된 스레드의 이름에 적용할 접두사
     * @param corePoolSize     스레드 풀에서 유지할 기본 스레드 수
     * @param maxPoolSize      스레드 풀에서 유지할 최대 스레드 수
     * @param queueCapacity    작업 요청을 저장할 대기열의 크기
     * @return 초기화된 ThreadPoolTaskExecutor 인스턴스
     */
    private ThreadPoolTaskExecutor createThreadPoolTaskExecutor(String threadNamePrefix, int corePoolSize, int maxPoolSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }
}
