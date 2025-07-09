package com.project200.undabang.policy.strategy;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 정책 유형(policy type)에 따라 적절한 {@link PolicyStrategy} 구현체를 찾아주는 컴포넌트입니다.
 * Spring 컨테이너에 등록된 모든 {@code PolicyStrategy} 빈을 수집하여 내부 맵에 저장하고,
 * 요청된 정책 유형에 맞는 전략 객체를 제공하는 역할을 합니다.
 */
@Component
public class PolicyStrategyFinder {
    private final Map<String, PolicyStrategy> policyStrategiesMap;

    /**
     * PolicyStrategyFinder를 생성하고, 주입된 모든 {@link PolicyStrategy} 구현체들을 맵에 등록합니다.
     * 각 전략의 {@code getPolicyType()} 메서드가 반환하는 문자열을 키로 사용하여 맵을 초기화합니다.
     * 키는 대소문자를 구분하지 않도록 소문자로 변환하여 저장합니다.
     */
    public PolicyStrategyFinder(List<PolicyStrategy> policyStrategyList) {
        Map<String, PolicyStrategy> map = new HashMap<>();

        for (PolicyStrategy strategy : policyStrategyList) {
            String type = strategy.getPolicyType();
            map.put(type.toLowerCase(), strategy);
        }

        this.policyStrategiesMap = map;
    }

    /**
     * 주어진 정책 유형 문자열에 해당하는 {@link PolicyStrategy} 구현체를 찾아서 반환합니다.
     */
    public PolicyStrategy findStrategy(String policyType) {
        PolicyStrategy policyStrategy = policyStrategiesMap.get(policyType.toLowerCase());
        if(policyStrategy == null){
            throw new CustomException(ErrorCode.POLICY_NOT_FOUND);
        }
        return policyStrategy;
    }
}
