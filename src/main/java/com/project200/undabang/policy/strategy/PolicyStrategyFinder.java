package com.project200.undabang.policy.strategy;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PolicyStrategyFinder {
    private final Map<String, PolicyStrategy> policyStrategiesMap;

    public PolicyStrategyFinder(List<PolicyStrategy> policyStrategyList) {
        Map<String, PolicyStrategy> map = new HashMap<>();

        for (PolicyStrategy strategy : policyStrategyList) {
            String type = strategy.getPolicyType();
            map.put(type.toLowerCase(), strategy);
        }

        this.policyStrategiesMap = map;
    }

    public PolicyStrategy findStrategy(String policyType) {
        PolicyStrategy policyStrategy = policyStrategiesMap.get(policyType.toLowerCase());
        if(policyStrategy == null){
            throw new CustomException(ErrorCode.POLICY_NOT_FOUND);
        }
        return policyStrategy;
    }
}
