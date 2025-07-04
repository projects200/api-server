package com.project200.undabang.common.batch.items;

import com.project200.undabang.common.batch.provider.DecreaseExerciseScoreQuerydslProvider;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@StepScope
@Component
public class DecreaseExerciseScoreReader extends JpaPagingItemReader<Member> { // @StepScope가 붙은 빈은 싱글톤이 아니라 Step 하나의 실행과정과 생명주기를 공유함
    private final PolicyService policyService;
    private final EntityManagerFactory entityManagerFactory;
    private final String runDate;
    private final int chunkSize;

    public DecreaseExerciseScoreReader(PolicyService policyService,
                                       EntityManagerFactory entityManagerFactory,
                                       @Value("#{jobParameters['runDate']}") String runDate,
                                       @Value("${batch.jobs.chunk-size}") int chunkSize){
        super();

        this.entityManagerFactory = entityManagerFactory;
        this.policyService = policyService;
        this.runDate = runDate;
        this.chunkSize = chunkSize;

        int THRESHOLD_DAYS = this.policyService.getPolicyAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);

        LocalDateTime referenceDate = LocalDate.parse(this.runDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                .atStartOfDay()
                .minusDays(THRESHOLD_DAYS);

        super.setName("decreaseExerciseReader");
        super.setEntityManagerFactory(this.entityManagerFactory);
        super.setPageSize(this.chunkSize);
        super.setQueryProvider(new DecreaseExerciseScoreQuerydslProvider(referenceDate));
    }
}
