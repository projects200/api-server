package com.project200.undabang.common.batch.config;

import com.project200.undabang.common.batch.items.DecreaseExerciseScoreProcessor;
import com.project200.undabang.common.batch.items.DecreaseExerciseScoreReader;
import com.project200.undabang.common.batch.items.DecreaseExerciseScoreWriter;
import com.project200.undabang.common.batch.listener.job.DecreaseExerciseScoreJobListener;
import com.project200.undabang.common.batch.listener.step.DecreaseExerciseScoreStepListener;
import com.project200.undabang.common.batch.provider.DecreaseExerciseScoreQuerydslProvider;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


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
    private final DecreaseExerciseScoreReader decreaseExerciseScoreReader;
    private final DecreaseExerciseScoreProcessor decreaseExerciseScoreProcessor;
    private final DecreaseExerciseScoreWriter decreaseExerciseScoreWriter;

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
                .reader(decreaseExerciseScoreReader)
                .processor(decreaseExerciseProcessor())
                .writer(decreaseExerciseWriter())
                .build();
    }


    /**
     * 운동 점수 감소 대상이 되는 회원 정보를 데이터베이스에서 읽어오는 ItemReader를 생성합니다.
     * StepScope 로 지정되어 각 Step 실행마다 새로운 인스턴스가 생성됩니다.
     * Job 파라미터로 받은 'runDate'를 기준으로 2주 이상 운동 기록이 없는 회원을 조회합니다.
     */
//    @Bean
    @StepScope
    public JpaPagingItemReader<Member> decreaseExerciseReader(@Value("#{jobParameters['runDate']}") String runDate){
        final int THRESHOLD_DAYS = policyService.getPolicyAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);

        LocalDateTime referenceDate = LocalDate.parse(runDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                .atStartOfDay()
                .minusDays(THRESHOLD_DAYS); // 이 부분 정책 테이블에서 가져와서 넣기 (추후 리팩토링 필요)

        return new JpaPagingItemReaderBuilder<Member>()
                .name("decreaseExerciseReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryProvider(new DecreaseExerciseScoreQuerydslProvider(referenceDate))
                .build();
    }

    /**
     * 읽어온 회원의 운동 점수를 감소시키는 ItemProcessor를 생성합니다.
     * 회원의 현재 점수가 0보다 큰 경우에만 DECREASE_SCORE 만큼 점수를 차감합니다.
     * 점수가 0인 회원은 변경되지 않습니다.
     */
//    @Bean
    public ItemProcessor<Member, Member> decreaseExerciseProcessor(){
        final int DECREASE_POINTS = policyService.getPolicyAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS);

        return member -> {
            byte currentMemberScore = member.getMemberScore();
            if(currentMemberScore > 0){
                log.info(">>>>>> 회원 점수 감소 대상: memberId = {}, memberNickname = {}, prevMemberScore = {}",
                        member.getMemberId(), member.getMemberNickname(), currentMemberScore);

                member.decreaseMemberScore(DECREASE_POINTS);

                return member;
            }
            return null;
        };
    }

    /**
     * 처리된 회원 정보를 데이터베이스에 저장하는 ItemWriter를 생성합니다.
     * JpaItemWriter를 사용하여 처리된 엔티티를 영속성 컨텍스트에 병합합니다.
     */
//    @Bean
    public JpaItemWriter<Member> decreaseExerciseWriter(){
        return new JpaItemWriterBuilder<Member>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
