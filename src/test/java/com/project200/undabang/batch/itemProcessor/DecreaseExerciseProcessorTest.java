package com.project200.undabang.batch.itemProcessor;

import com.project200.undabang.common.batch.config.DecreaseExerciseScoreJobConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.item.ItemProcessor;

class DecreaseExerciseProcessorTest {
    private ItemProcessor<Member, Member> processor;
    private PolicyService policyService;

    @BeforeEach
    void setUp(){
        // PolicyService Mock 객체 생성
        policyService = Mockito.mock(PolicyService.class);
        Mockito.when(policyService.getPolicyAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).thenReturn(1);

        // 설정된 Mock 객체를 사용해서 DecreaseExerciseScoreJobConfig 인스턴스 생성
        // processor가 policyService외 다른 의존성을 사용하지 않으니 null 전달 가능
        DecreaseExerciseScoreJobConfig config  = new DecreaseExerciseScoreJobConfig(null, null, null,
                null, null, policyService);
        processor = config.decreaseExerciseProcessor();
    }

    @Test
    @DisplayName("회원의 점수가 0보다 크면 1을 감소해야 한다")
    void memberScoreDecreaseProcessor_whenScorePositive() throws Exception {
        // given
        byte initialScore = 1;
        Member member = Member.builder().memberScore(initialScore).build();

        // when
        Member processedMember = processor.process(member);

        // then
        Assertions.assertThat(processedMember).isNotNull();
        Assertions.assertThat(processedMember.getMemberScore()).isEqualTo((byte) 0);
    }

    @Test
    @DisplayName("회원의 점수가 0이면 null을 반환해야 한다")
    void memberScoreDecreaseProcessor_whenScoreIsZero() throws Exception{
        // given
        Member member = Member.builder().memberScore((byte) 0).build();

        // when
        Member processedMember = processor.process(member);

        // then
        Assertions.assertThat(processedMember).isNull();
    }

}
