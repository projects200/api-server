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
     * 이 메소드들은 StepScope 빈으로, 실제 작업(Step)이 시작될 때 마다 새로 생성해야 하는 빈임.
     * 그런데 싱글톤 (@Configuration) 빈이 StepScope 빈을 요청하다 보니 Proxy 객체를 대신 주게 됨
     * 이때, 프록시 객체를 제대로 전달해 주지 못해서 초기화가 실패해 JpaQueryFactory 대신에 Null이 Reader에 계속 주입되는 상황 발생
     * 따라서 @Bean 메소드로 해당 Item 들을 관리하여, Step이 해당 빈들을 필요로 할때 생성하도록 변경함
     * <p>
     * 싱글톤 빈이 StepScope 빈을 주입받을 때는, 필드 주입이 아니라 @Bean 메서드 호출 방식을 사용해 "필요할때 만들도록" 해야 함
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
