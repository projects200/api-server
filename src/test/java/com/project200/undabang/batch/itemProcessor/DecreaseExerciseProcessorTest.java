package com.project200.undabang.batch.itemProcessor;

import com.project200.undabang.batch.config.DecreaseExerciseScoreJobConfig;
import com.project200.undabang.member.entity.Member;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ItemProcessor;

class DecreaseExerciseProcessorTest {
    private final ItemProcessor<Member, Member> processor = new DecreaseExerciseScoreJobConfig(null, null, null, null, null)
            .decreaseExerciseProcessor();

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
