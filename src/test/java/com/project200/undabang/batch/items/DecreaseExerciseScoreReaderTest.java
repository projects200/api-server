package com.project200.undabang.batch.items;

import com.project200.undabang.common.batch.items.DecreaseExerciseScoreReader;
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
    @DisplayName("주입된 값들을 기반으로 QueryProvider가 올바른 referenceDate로 생성되는지 테스트")
    void reader_initializes_queryProvider_correctly() throws Exception {
        // given
        String runDate = "2023-10-15";
        int chunkSize = 10;
        int thresholdDays = 7;

        Mockito.when(policyService.getPolicyAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS))
                .thenReturn(thresholdDays);

        // when
        DecreaseExerciseScoreReader reader = new DecreaseExerciseScoreReader(
                policyService, entityManagerFactory, runDate, chunkSize
        );


        // then
        // private 필드인 queryProvider를 ReflectionTestUtils로 꺼내서 검증
        DecreaseExerciseScoreQuerydslProvider provider =
                (DecreaseExerciseScoreQuerydslProvider) ReflectionTestUtils.getField(reader, "queryProvider");

        // provider 내부의 referenceDate를 꺼내서 검증
        LocalDateTime expectedReferenceDate = LocalDate.parse(runDate).atStartOfDay().minusDays(thresholdDays);
        LocalDateTime actualReferenceDate =
                (LocalDateTime) ReflectionTestUtils.getField(provider, "referenceDate");

        Assertions.assertThat(actualReferenceDate).isEqualTo(expectedReferenceDate);
    }

    @Test
    @DisplayName("엣지 케이스: threshold가 0일 때 referenceDate가 runDate와 동일하게 계산된다")
    void reader_calculates_referenceDate_correctly_when_threshold_is_zero() throws Exception {
        // given
        String runDate = "2023-10-15";
        int chunkSize = 10;
        int thresholdDays = 0;

        Mockito.when(policyService.getPolicyAsInt(PolicyKey.PENALTY_INACTIVITY_THRESHOLD_DAYS)).thenReturn(thresholdDays);

        // when
        DecreaseExerciseScoreReader reader = new DecreaseExerciseScoreReader(
                policyService, entityManagerFactory, runDate, chunkSize
        );
        // then
        DecreaseExerciseScoreQuerydslProvider provider =
                (DecreaseExerciseScoreQuerydslProvider) ReflectionTestUtils.getField(reader, "queryProvider");
        LocalDateTime actualReferenceDate =
                (LocalDateTime) ReflectionTestUtils.getField(provider, "referenceDate");

        Assertions.assertThat(actualReferenceDate).isEqualTo(LocalDate.parse(runDate).atStartOfDay());
    }
}
