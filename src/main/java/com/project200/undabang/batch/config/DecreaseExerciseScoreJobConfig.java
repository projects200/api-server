package com.project200.undabang.batch.config;

import com.project200.undabang.batch.listener.job.DecreaseExerciseScoreJobListener;
import com.project200.undabang.batch.listener.step.DecreaseExerciseScoreStepListener;
import com.project200.undabang.batch.provider.DecreaseExerciseScoreQuerydslProvider;
import com.project200.undabang.member.entity.Member;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DecreaseExerciseScoreJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DecreaseExerciseScoreJobListener decreaseExerciseScoreJobListener;
    private final DecreaseExerciseScoreStepListener decreaseExerciseScoreStepListener;

    private static final int CHUNK_SIZE = 100; // 10, 100, 1000 중 뭐가 좋을진 아직 모르겠음 (데이터 부족)
    private static final byte DECREASE_SCORE = 1; // 언제든 정책에 따라 바꿀 수 있도록 전역 변수로 설계

    @Bean
    public Job decreaseExerciseScoreJob(){
        return new JobBuilder("decreaseExerciseScoreJob", jobRepository)
                .listener(decreaseExerciseScoreJobListener)
                .start(decreaseExerciseScoreStep())
                .build();
    }

    @Bean
    public Step decreaseExerciseScoreStep(){
        return new StepBuilder("decreaseExerciseScoreStep", jobRepository)
                .<Member, Member>chunk(CHUNK_SIZE, platformTransactionManager)
                .listener(decreaseExerciseScoreStepListener)
                .reader(decreaseExerciseReader(null))
                .processor(decreaseExerciseProcessor())
                .writer(decreaseExerciseWriter())
                .build();
    }


    @Bean
    @StepScope
    public JpaPagingItemReader<Member> decreaseExerciseReader(@Value("#{jobParameters['runDate']}") String runDate){
        LocalDateTime referenceDate = LocalDate.parse(runDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                .atStartOfDay()
                .minusWeeks(2);

        return new JpaPagingItemReaderBuilder<Member>()
                .name("decreaseExerciseReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryProvider(new DecreaseExerciseScoreQuerydslProvider(referenceDate))
                .build();
    }

    @Bean
    public ItemProcessor<Member, Member> decreaseExerciseProcessor(){
        return member -> {
            byte currentMemberScore = member.getMemberScore();
            if(currentMemberScore > 0){
                log.info(">>>>>> 회원 점수 감소 대상: memberId = {}, memberNickname = {}, prevMemberScore = {}",
                        member.getMemberId(), member.getMemberNickname(), currentMemberScore);

                member.decreaseMemberScore(DECREASE_SCORE);

                return member;
            }
            return null;
        };
    }

    @Bean
    public JpaItemWriter<Member> decreaseExerciseWriter(){
        return new JpaItemWriterBuilder<Member>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
