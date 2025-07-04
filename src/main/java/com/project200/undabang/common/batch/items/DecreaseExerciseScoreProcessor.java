package com.project200.undabang.common.batch.items;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class DecreaseExerciseScoreProcessor implements ItemProcessor<Member, Member> {
    private final int decreasePoint;

    public DecreaseExerciseScoreProcessor(PolicyService policyService){
        this.decreasePoint = policyService.getPolicyAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS);
    }

    @Override
    public Member process(Member item) throws Exception {
        return null;
    }
}
