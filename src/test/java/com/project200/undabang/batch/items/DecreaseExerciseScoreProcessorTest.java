package com.project200.undabang.batch.items;

import com.project200.undabang.common.batch.items.DecreaseExerciseScore.DecreaseExerciseScoreProcessor;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DecreaseExerciseScoreProcessorTest {
    @Mock
    private PolicyService policyService;

    @InjectMocks
    private DecreaseExerciseScoreProcessor processor;

    private int DECREASE_POINT;

    @BeforeEach
    void setUp(){
        Mockito.when(policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS)).thenReturn(DECREASE_POINT);
        DECREASE_POINT = policyService.getPolicyValueAsInt(PolicyKey.PENALTY_SCORE_DECREMENT_POINTS);
    }

    @Test
    @DisplayName("회원의 점수가 0보다 크면 1을 감소해야 한다")
    void memberScoreDecreaseProcessor_whenScorePositive() throws Exception {
        // given
        Member testMember = Mockito.mock(Member.class);
        Mockito.when(testMember.getMemberScore()).thenReturn((byte) 77);

        // when
        Member result = processor.process(testMember);

        // then
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).isEqualTo(testMember);

        Mockito.verify(testMember, Mockito.times(1)).decreaseMemberScore(DECREASE_POINT);
    }

    @Test
    @DisplayName("회원의 점수가 0이면 null을 반환해야 한다")
    void memberScoreDecreaseProcessor_whenScoreIsZero() throws Exception{
        // given
        Member mockMember = Mockito.mock(Member.class);
        Mockito.when(mockMember.getMemberScore()).thenReturn((byte) 0);

        // when
        Member result = processor.process(mockMember);

        // then
        Assertions.assertThat(result).isNull();

        Mockito.verify(mockMember, Mockito.never()).decreaseMemberScore(Mockito.anyInt());
    }
}
