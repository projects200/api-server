package com.project200.undabang.score.service.impl;

import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.score.validation.ExercisePolicyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ExercisePolicyValidatorImpl implements ExercisePolicyValidator {

    private final PolicyService policyService;

    /**
     * 운동 기록후 점수 획득이 가능한 유효 기간을 계산합니다.
     * 정책에 정의된 기간만큼 현재 시간에서 빼서 유효 종료 시간을 반환합니다.
     *
     * @return 유효 종료 시간
     */
    public LocalDateTime calculateValidityEndDate() {
        Policy validityPolicy = policyService.getPolicy(PolicyKey.EXERCISE_RECORD_VALIDITY_PERIOD);
        long periodValue = Long.parseLong(validityPolicy.getPolicyValue());
        String periodUnit = validityPolicy.getPolicyUnit();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validUntil = switch (periodUnit.toUpperCase()) {
            case "DAYS" -> now.minusDays(periodValue).withHour(0).withMinute(0).withSecond(0);
            case "HOURS" -> now.minusHours(periodValue).withMinute(0).withSecond(0);
            case "MINUTES" -> now.minusMinutes(periodValue).withSecond(0);
            default -> throw new IllegalStateException("Unsupported policy unit: " + periodUnit);
        };
        return validUntil;
    }
}