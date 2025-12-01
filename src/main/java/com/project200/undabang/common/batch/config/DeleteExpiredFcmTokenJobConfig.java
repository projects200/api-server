package com.project200.undabang.common.batch.config;

import com.project200.undabang.common.batch.items.DeleteExpiredFcmToken.DeleteExpiredFcmTokenReader;
import com.project200.undabang.common.batch.items.DeleteExpiredFcmToken.DeleteExpiredFcmTokenWriter;
import com.project200.undabang.common.batch.listener.job.DeleteExpiredFcmTokenJobListener;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeleteExpiredFcmTokenJobConfig {
    private final FcmTokenRepository fcmTokenRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final JobRepository jobRepository;
    private final DeleteExpiredFcmTokenJobListener deleteExpiredFcmTokenJobListener;

    /**
     * 한 번에 처리할 데이터의 양(청크)을 지정합니다.
     * 데이터베이스 트랜잭션은 청크 단위로 커밋됩니다.
     */
    @Value("${batch.jobs.chunk-size}")
    private int CHUNK_SIZE;

    @Bean
    public Job deleteExpiredFcmTokenJob() {
        return new JobBuilder("deleteExpiredFcmTokenJob", jobRepository)
                .listener(deleteExpiredFcmTokenJobListener)
                .start(deleteExpiredFcmTokenStep())
                .build();
    }

    @Bean
    public Step deleteExpiredFcmTokenStep() {
        return new StepBuilder("deleteExpiredFcmTokenStep", jobRepository)
                .<Long, Long>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(expiredTokenIdReader())
                .writer(expiredTokenIdWriter())
                .build();
    }

    /**
     * Reader를 StepScope 빈으로 정의합니다.
     * Step 실행 시마다 새로운 인스턴스를 생성하여 상태를 격리하고,
     * Job Parameters 등의 Step 컨텍스트 정보를 주입받을 수 있도록 합니다.
     */
    @Bean
    @StepScope
    public DeleteExpiredFcmTokenReader expiredTokenIdReader() {
        return new DeleteExpiredFcmTokenReader(fcmTokenRepository, CHUNK_SIZE);
    }

    @Bean
    public DeleteExpiredFcmTokenWriter expiredTokenIdWriter() {
        return new DeleteExpiredFcmTokenWriter(fcmTokenRepository);
    }
}
