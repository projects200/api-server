package com.project200.undabang.common.batch.items;

import com.project200.undabang.common.batch.provider.DecreaseExerciseScoreQuerydslProvider;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * 장기간 활동이 없는 회원을 조회하여 운동 점수를 감소시키는 배치 작업의 ItemReader 구현체입니다.
 * JpaPagingItemReader를 상속받아 페이징 처리 방식으로 대량의 회원 데이터를 효율적으로 읽어옵니다.
 * 이 리더는 'runDate' 잡 파라미터를 기준으로 PENALTY_INACTIVITY_THRESHOLD_DAYS 정책에서
 * 정의한 기간 동안 활동이 없는 회원을 대상으로 합니다.
 * 조회된 Member 엔티티는 DecreaseExerciseScoreProcessor로 전달되어 점수 감소 로직이 처리됩니다.
 *
 * @StepScope가 붙은 빈은 싱글톤이 아니라 Step 하나의 실행과정과 생명주기를 공유한다
 */
@Slf4j
//@StepScope
public class DecreaseExerciseScoreReader extends JpaPagingItemReader<Member> {
    private final DecreaseExerciseScoreQuerydslProvider querydslProvider;

    public DecreaseExerciseScoreReader(PolicyService policyService,
                                       EntityManagerFactory entityManagerFactory,
                                       @Value("#{jobParameters['runDate']}") String runDate,
                                       @Value("${batch.jobs.chunk-size}") int chunkSize){

        int THRESHOLD_DAYS = policyService.getPolicyAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS);

        LocalDateTime referenceDate = LocalDate.parse(runDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                .atStartOfDay()
                .minusDays(THRESHOLD_DAYS);

        this.querydslProvider = new DecreaseExerciseScoreQuerydslProvider(referenceDate);

        super.setName("decreaseExerciseScoreReader");
        super.setEntityManagerFactory(entityManagerFactory);
        super.setPageSize(chunkSize);
        super.setQueryProvider(this.querydslProvider);
    }

    /**
     * JpaPagingItemReader의 내부 동작을 보면, EntityManager가 QueryProvider에 연결될 때, afterPropertiesSet()을 호출해야 한다.
     * 하지만 afterPropertiesSet 을 @Override해서 재정의 하였다.
     * 그러다 보니 EntityManager 는 read()가 처음 호출될 때 실행되는 doOpen() 메서드 안에서 QueryProvider에게 전달되는데, afterPropertiesSet()은 호출되지 않았으므로,
     * JpaQueryFactory 는 생성되지 못해서 Null 값이 계속 전달된 것이다.
     *
     * 따라서 super.doOpen()을 먼저 호출하여 의존관계를 주입한 후, afterPropertiesSet()을 호출하여 내부 동작 순서를 맞춰주어야 한다.
     */
    @Override
    protected void doOpen() throws Exception {
        super.doOpen();

        if(this.querydslProvider != null){
            this.querydslProvider.afterPropertiesSet();
        }else{
            throw new IllegalStateException("querydslProvider is null!");
        }
    }
}
