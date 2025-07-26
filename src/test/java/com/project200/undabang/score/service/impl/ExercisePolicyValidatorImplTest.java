package com.project200.undabang.score.service.impl;

import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.within;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExercisePolicyValidatorImpl 테스트")
class ExercisePolicyValidatorImplTest {

    @Mock
    private PolicyService policyService;

    @InjectMocks
    private ExercisePolicyValidatorImpl exercisePolicyValidator;

    // --- 테스트 대상 메서드별로 그룹화 ---
    @Nested
    @DisplayName("calculateValidityEndDate 메서드는")
    class Describe_calculateValidityEndDate {

        @Test
        @DisplayName("정책 단위가 'DAYS'일 때, 현재 시간에서 정책 값만큼의 날짜를 뺀 시간을 정확히 반환한다")
        void it_returns_correct_date_when_unit_is_days() {
            // given: "2일 전" 정책을 설정합니다.
            long periodValue = 2L;
            Policy daysPolicy = Policy.builder()
                    .policyKey(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD)
                    .policyValue(String.valueOf(periodValue))
                    .policyUnit("DAYS")
                    .policyDescription("")
                    .build();

            // policyService.getPolicy가 호출되면 위에서 만든 policy 객체를 반환하도록 설정합니다.
            given(policyService.getPolicy(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD))
                    .willReturn(daysPolicy);

            // when: 유효 기간 종료일을 계산합니다.
            LocalDateTime result = exercisePolicyValidator.calculateValidityEndDate();

            // then: 결과가 현재 시간으로부터 정확히 2일 전인지 확인합니다. (초 단위 절삭)
            LocalDateTime expected = LocalDateTime.now().minusDays(periodValue).truncatedTo(ChronoUnit.DAYS);
            assertThat(result).isCloseTo(expected, within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("정책 단위가 'HOURS'일 때, 현재 시간에서 정책 값만큼의 시간을 뺀 시간을 정확히 반환한다")
        void it_returns_correct_date_when_unit_is_hours() {
            // given: "24시간 전" 정책을 설정합니다.
            long periodValue = 24L;
            Policy hoursPolicy = Policy.builder()
                    .policyKey(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD)
                    .policyValue(String.valueOf(periodValue))
                    .policyUnit("HOURS")
                    .policyDescription("")
                    .build();
            given(policyService.getPolicy(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD))
                    .willReturn(hoursPolicy);

            // when
            LocalDateTime result = exercisePolicyValidator.calculateValidityEndDate();

            // then: 결과가 현재 시간으로부터 정확히 5시간 전인지 확인합니다. (분 단위 절삭)
            LocalDateTime expected = LocalDateTime.now().minusHours(periodValue).truncatedTo(ChronoUnit.HOURS);
            assertThat(result).isCloseTo(expected, within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("정책 단위가 'MINUTES'일 때, 현재 시간에서 정책 값만큼의 분을 뺀 시간을 정확히 반환한다")
        void it_returns_correct_date_when_unit_is_minutes() {
            // given: "30분 전" 정책을 설정합니다.
            long periodValue = 30L;
            Policy minutesPolicy = Policy.builder()
                    .policyKey(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD)
                    .policyValue(String.valueOf(periodValue))
                    .policyUnit("MINUTES")
                    .policyDescription("")
                    .build();
            given(policyService.getPolicy(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD))
                    .willReturn(minutesPolicy);

            // when
            LocalDateTime result = exercisePolicyValidator.calculateValidityEndDate();

            // then: 결과가 현재 시간으로부터 정확히 30분 전인지 확인합니다. (초 단위 절삭)
            LocalDateTime expected = LocalDateTime.now().minusMinutes(periodValue).truncatedTo(ChronoUnit.MINUTES);
            assertThat(result).isCloseTo(expected, within(1, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("지원하지 않는 정책 단위를 사용하면 IllegalStateException 예외를 발생시킨다")
        void it_throws_illegal_state_exception_for_unsupported_unit() {
            // given: 지원하지 않는 "WEEKS" 단위를 설정합니다.
            Policy unsupportedPolicy = Policy.builder()
                    .policyKey(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD)
                    .policyValue("1")
                    .policyUnit("WEEKS")
                    .policyDescription("")
                    .build();
            given(policyService.getPolicy(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD))
                    .willReturn(unsupportedPolicy);

            // when & then: 예외가 발생하는지 확인합니다.
            assertThatThrownBy(() -> exercisePolicyValidator.calculateValidityEndDate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unsupported policy unit: WEEKS");
        }
    }
}