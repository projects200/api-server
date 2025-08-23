package com.project200.undabang.timer.custom.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepository;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class CustomTimerStepRepositoryImplTest {
    @Autowired
    private EntityManager em;

    @Autowired
    private CustomTimerStepRepository customTimerStepRepository;

    private Member createAndSaveMember(String nickname) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@email.com")
                .memberNickname(nickname)
                .memberGender(MemberGender.UNKNOWN) // 예시 값
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
        em.persist(member);
        return member;
    }

    private CustomTimer createAndSaveCustomTimer(Member member, String name) {
        CustomTimer timer = CustomTimer.builder()
                .member(member)
                .customTimerName(name)
                .build();
        em.persist(timer);
        return timer;
    }

    private CustomTimerStep createAndSaveCustomTimerStep(CustomTimer timer, String name, byte order) {
        CustomTimerStep step = CustomTimerStep.builder()
                .customTimer(timer)
                .customTimerStepName(name)
                .customTimerStepOrder(order)
                .customTimerStepTime(60) // 기본값
                .build();
        em.persist(step);
        return step;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("softDeleteAllByCustomTimer 메소드는")
    class Describe_softDeleteAllByCustomTimer {

        @Test
        @DisplayName("주어진 타이머에 속한 활성 상태의 스텝들만 논리적으로 삭제한다")
        void it_soft_deletes_only_active_steps_for_the_given_timer() {
            // given
            Member member = createAndSaveMember("testUser");
            CustomTimer timerA = createAndSaveCustomTimer(member, "timerA");
            CustomTimer timerB = createAndSaveCustomTimer(member, "timerB"); // 영향을 받지 않아야 할 다른 타이머

            // timerA에 속한 스텝들 (활성 2개, 이미 삭제된 것 1개)
            CustomTimerStep stepA1 = createAndSaveCustomTimerStep(timerA, "stepA1", (byte) 0);
            CustomTimerStep stepA2 = createAndSaveCustomTimerStep(timerA, "stepA2", (byte) 1);
            CustomTimerStep stepA3_deleted = createAndSaveCustomTimerStep(timerA, "stepA3_deleted", (byte) 2);
            stepA3_deleted.deleteCustomTimerStep(); // 미리 삭제 처리

            // timerB에 속한 스텝
            CustomTimerStep stepB1 = createAndSaveCustomTimerStep(timerB, "stepB1", (byte) 0);

            flushAndClear();

            // when
            customTimerStepRepository.softDeleteAllByCustomTimer(timerA);

            // then
            // 메소드 내부에서 clear()가 호출되므로, DB에서 직접 상태를 다시 조회하여 검증해야 함
            CustomTimerStep foundStepA1 = em.find(CustomTimerStep.class, stepA1.getId());
            CustomTimerStep foundStepA2 = em.find(CustomTimerStep.class, stepA2.getId());
            CustomTimerStep foundStepA3 = em.find(CustomTimerStep.class, stepA3_deleted.getId());
            CustomTimerStep foundStepB1 = em.find(CustomTimerStep.class, stepB1.getId());

            Assertions.assertThat(foundStepA1.getCustomTimerStepDeletedAt()).isNotNull();
            Assertions.assertThat(foundStepA2.getCustomTimerStepDeletedAt()).isNotNull();
            Assertions.assertThat(foundStepA3.getCustomTimerStepDeletedAt()).isNotNull(); // 원래 삭제된 상태 유지
            Assertions.assertThat(foundStepB1.getCustomTimerStepDeletedAt()).isNull(); // 다른 타이머 스텝은 영향 없음
        }

        @Test
        @DisplayName("스텝이 없는 타이머에 대해 호출해도 아무런 작업을 수행하지 않고 오류를 발생시키지 않는다")
        void it_does_nothing_when_timer_has_no_steps() {
            // given
            Member member = createAndSaveMember("testUser");
            CustomTimer timerWithNoSteps = createAndSaveCustomTimer(member, "emptyTimer");
            flushAndClear();

            // when
            // 예외가 발생하지 않으면 성공
            customTimerStepRepository.softDeleteAllByCustomTimer(timerWithNoSteps);

            // then
            List<CustomTimerStep> steps = customTimerStepRepository.findAll();
            Assertions.assertThat(steps).isEmpty();
        }
    }
}