package com.project200.undabang.common.batch.items;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;

/**
 * '운동 점수 감소' 배치 작업의 ItemProcessor 구현체입니다.
 * DecreaseExerciseScoreReader로부터 전달받은 Member 객체를 처리합니다.
 * 이 프로세서는 회원의 현재 운동 점수가 0보다 큰 경우에만 정책에 정의된 점수만큼 차감합니다.
 * 만약 회원의 점수가 0 이하라면, 해당 회원은 처리에서 제외(필터링)되어 ItemWriter로 전달되지 않습니다.
 * 처리된 Member 객체는 DecreaseExerciseScoreWriter로 전달되어 데이터베이스에 최종적으로 업데이트됩니다.
 */
@Slf4j
@StepScope  // 코드의 명시성을 위해 유지 (실제 적용은 @Bean 에서 적용됨)
public class DecreaseExerciseScoreProcessor implements ItemProcessor<Member, Member> {
    private final int decreasePoints;

    public DecreaseExerciseScoreProcessor(PolicyService policyService){
        this.decreasePoints = policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS);
    }

    @Override
    public Member process(Member item) throws Exception {
        byte currentMemberScore = item.getMemberScore();
        if(currentMemberScore > 0){
            log.info(">>>>>>> 회원 점수 감소 대상 : memberId = {}, memberNickName = {}, prevMemberScore = {}",
                    item.getMemberId(), item.getMemberNickname(), currentMemberScore);

            item.decreaseMemberScore(decreasePoints);

            return item;
        }
        return null;
    }
}
