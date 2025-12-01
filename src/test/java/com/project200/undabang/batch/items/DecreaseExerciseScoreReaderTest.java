package com.project200.undabang.batch.items;

import com.project200.undabang.common.batch.items.DecreaseExerciseScore.DecreaseExerciseScoreReader;
import com.project200.undabang.common.batch.provider.DecreaseExerciseScoreQuerydslProvider;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class DecreaseExerciseScoreReaderTest {
    @Mock
    private PolicyService policyService;

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("타임스탬프 형식의 runDate를 기반으로 QueryProvider가 올바른 referenceDate로 생성되는지 테스트")
    void reader_initializes_queryProvider_correctly() {
        // given
        // 1. runDate를 실제 JobParameter와 같은 타임스탬프 형식으로 변경
        String runDate = "2023-10-15T10:30:00.123";
        int chunkSize = 10;
        int thresholdDays = 7;

        Mockito.when(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS))
                .thenReturn(thresholdDays);

        // when
        DecreaseExerciseScoreReader reader = new DecreaseExerciseScoreReader(
                policyService, entityManagerFactory, runDate, chunkSize
        );


        // then
        DecreaseExerciseScoreQuerydslProvider provider =
                (DecreaseExerciseScoreQuerydslProvider) ReflectionTestUtils.getField(reader, "queryProvider");

        // 2. expectedReferenceDate 계산 로직을 실제 Reader의 로직과 동일하게 수정
        //    LocalDateTime으로 파싱 -> LocalDate 추출 -> 날짜 계산
        LocalDate jobDate = LocalDateTime.parse(runDate).toLocalDate();
        LocalDateTime expectedReferenceDate = jobDate.atStartOfDay().minusDays(thresholdDays);

        LocalDateTime actualReferenceDate =
                (LocalDateTime) ReflectionTestUtils.getField(provider, "referenceDate");

        Assertions.assertThat(actualReferenceDate).isEqualTo(expectedReferenceDate);
    }

    @Test
    @DisplayName("엣지 케이스: threshold가 0일 때 referenceDate가 runDate의 날짜와 동일하게 계산된다")
    void reader_calculates_referenceDate_correctly_when_threshold_is_zero() {
        // given
        // 1. runDate를 실제 JobParameter와 같은 타임스탬프 형식으로 변경
        String runDate = "2023-10-15T23:59:59.999";
        int chunkSize = 10;
        int thresholdDays = 0;

        Mockito.when(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).thenReturn(thresholdDays);

        // when
        DecreaseExerciseScoreReader reader = new DecreaseExerciseScoreReader(
                policyService, entityManagerFactory, runDate, chunkSize
        );

        // then
        DecreaseExerciseScoreQuerydslProvider provider =
                (DecreaseExerciseScoreQuerydslProvider) ReflectionTestUtils.getField(reader, "queryProvider");

        // 2. expectedReferenceDate 계산 로직을 실제 Reader의 로직과 동일하게 수정
        LocalDate jobDate = LocalDateTime.parse(runDate).toLocalDate();
        LocalDateTime expectedReferenceDate = jobDate.atStartOfDay();

        LocalDateTime actualReferenceDate =
                (LocalDateTime) ReflectionTestUtils.getField(provider, "referenceDate");

        Assertions.assertThat(actualReferenceDate).isEqualTo(expectedReferenceDate);
    }
}
