package com.project200.undabang.batch.config;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DecreaseExerciseScoreJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;

    private static final int CHUNK_SIZE = 10; // 10, 100, 1000 중 뭐가 좋을진 아직 모르겠음 (데이터 부족)

    @Bean
    public Job decreaseExerciseScoreJob(){
        return new JobBuilder("decreaseExerciseScoreJob", jobRepository)
                .start(decreaseExerciseScoreStep())
                .build();
    }

    @Bean
    public Step decreaseExerciseScoreStep(){
        return new StepBuilder("decreaseExerciseScoreStep", jobRepository)
                .<Member, Member>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(decreaseExerciseReader())
                .processor(decreaseExerciseProcessor())
                .writer(decreaseExerciseWriter())
                .build();
    }

    // jpql로 쿼리를 작성하는것 보다 Querydsl을 쓰는게 더 좋다고 판단하여 queryProvider 사용
    @Bean
    public JpaPagingItemReader<Member> decreaseExerciseReader(){
        return new JpaPagingItemReaderBuilder<Member>()
                .name("decreaseExerciseReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryProvider(new MemberScoreQuerydslProvider())
                .build();
    }
}
