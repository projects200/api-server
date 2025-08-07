package com.project200.undabang.common.batch.config;

import com.project200.undabang.common.batch.items.DecreaseExerciseScoreProcessor;
import com.project200.undabang.common.batch.items.DecreaseExerciseScoreReader;
import com.project200.undabang.common.batch.items.DecreaseExerciseScoreWriter;
import com.project200.undabang.common.batch.listener.job.DecreaseExerciseScoreJobListener;
import com.project200.undabang.common.batch.listener.step.DecreaseExerciseScoreStepListener;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.service.PolicyService;
import jakarta.persistence.EntityManagerFactory;
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


/**
 * 장기간 운동을 하지 않은 회원의 운동 점수를 감소시키는 배치 잡(Batch Job)의 설정 클래스입니다.
 * 이 설정은 Job, Step, ItemReader, ItemProcessor, ItemWriter를 정의합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DecreaseExerciseScoreJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DecreaseExerciseScoreJobListener decreaseExerciseScoreJobListener;
    private final DecreaseExerciseScoreStepListener decreaseExerciseScoreStepListener;
    private final PolicyService policyService;

    /**
     * 한 번에 처리할 데이터의 양(청크)을 지정합니다.
     * 데이터베이스 트랜잭션은 청크 단위로 커밋됩니다.
     */
    @Value("${batch.jobs.chunk-size}")
    private int CHUNK_SIZE;

    /**
     * '운동 점수 감소' Job을 생성하여 빈으로 등록합니다.
     * Job은 하나의 Step으로 구성됩니다.
     */
    @Bean
    public Job decreaseExerciseScoreJob(){
        return new JobBuilder("decreaseExerciseScoreJob", jobRepository)
                .listener(decreaseExerciseScoreJobListener)
                .start(decreaseExerciseScoreStep())
                .preventRestart() // Job 재시작 방지
                .build();
    }

    /**
     * '운동 점수 감소' Step을 생성하여 빈으로 등록합니다.
     * 이 Step은 decreaseExerciseReader를 통해 대상 회원을 읽고,
     * decreaseExerciseProcessor에서 점수를 감소시킨 후,
     * decreaseExerciseWriter를 통해 변경된 회원 정보를 데이터베이스에 저장합니다.
     */
    @Bean
    public Step decreaseExerciseScoreStep(){
        return new StepBuilder("decreaseExerciseScoreStep", jobRepository)
                .<Member, Member>chunk(CHUNK_SIZE, platformTransactionManager)
                .listener(decreaseExerciseScoreStepListener)
                .reader(decreaseExerciseScoreReader(null, CHUNK_SIZE))
                .processor(decreaseExerciseScoreProcessor())
                .writer(decreaseExerciseScoreWriter())
                .build();
    }

    /**
     * 이 메소드들은 StepScope 빈으로, 실제 작업(Step)이 시작될 때 마다 새로 생성해야 하는 빈임.
     * 그런데 싱글톤 (@Configuration) 빈이 StepScope 빈을 요청하다 보니 Proxy 객체를 대신 주게 됨
     * 이때, 프록시 객체를 제대로 전달해 주지 못해서 초기화가 실패해 JpaQueryFactory 대신에 Null이 Reader에 계속 주입되는 상황 발생
     * 따라서 @Bean 메소드로 해당 Item 들을 관리하여, Step이 해당 빈들을 필요로 할때 생성하도록 변경함
     *
     * 싱글톤 빈이 StepScope 빈을 주입받을 때는, 필드 주입이 아니라 @Bean 메서드 호출 방식을 사용해 "필요할때 만들도록" 해야 함
     */
    @Bean
    @StepScope
    public DecreaseExerciseScoreReader decreaseExerciseScoreReader(@Value("#{jobParameters['runDate']}") String runDate,
                                                              @Value("${batch.jobs.chunk-size}") int chunkSize){
        return new DecreaseExerciseScoreReader(policyService, entityManagerFactory, runDate, chunkSize);
    }

    @Bean
    @StepScope
    public DecreaseExerciseScoreProcessor decreaseExerciseScoreProcessor() {
        return new DecreaseExerciseScoreProcessor(policyService);
    }

    @Bean
    public DecreaseExerciseScoreWriter decreaseExerciseScoreWriter() {
        return new DecreaseExerciseScoreWriter(entityManagerFactory);
    }
}
