package com.project200.undabang.timer.custom.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
        em.persist(member);
        return member;
    }

    private CustomTimer createAndSaveCustomTimer(Member member, String name) {
        CustomTimer timer = CustomTimer.of(member, name);
        em.persist(timer);
        return timer;
    }

    private CustomTimerStep createAndSaveCustomTimerStep(CustomTimer timer, String name, byte order) {
        CustomTimerStep step = CustomTimerStep.of(timer, name, order, 60);
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
            CustomTimer timerB = createAndSaveCustomTimer(member, "timerB");

            createAndSaveCustomTimerStep(timerA, "stepA1", (byte) 0);
            createAndSaveCustomTimerStep(timerA, "stepA2", (byte) 1);
            createAndSaveCustomTimerStep(timerA, "stepA3", (byte) 2);
            createAndSaveCustomTimerStep(timerB, "stepB1", (byte) 0);

            flushAndClear();

            // when
            customTimerStepRepository.softDeleteAllByCustomTimer(timerA);

            // then
            List<CustomTimerStep> timerASteps = customTimerStepRepository.findAll().stream()
                    .filter(s -> s.getCustomTimer().getId().equals(timerA.getId()))
                    .toList();
            List<CustomTimerStep> timerBSteps = customTimerStepRepository.findAll().stream()
                    .filter(s -> s.getCustomTimer().getId().equals(timerB.getId()))
                    .toList();

            assertThat(timerASteps).hasSize(3);
            for (CustomTimerStep step : timerASteps) {
                assertThat(step.getCustomTimerStepDeletedAt()).isNotNull();
            }
            assertThat(timerBSteps).hasSize(1);
            assertThat(timerBSteps.get(0).getCustomTimerStepDeletedAt()).isNull();
        }

        @Test
        @DisplayName("스텝이 없는 타이머에 대해 호출해도 아무런 작업을 수행하지 않고 오류를 발생시키지 않는다")
        void it_does_nothing_when_timer_has_no_steps() {
            // given
            Member member = createAndSaveMember("testUser");
            CustomTimer timerWithNoSteps = createAndSaveCustomTimer(member, "emptyTimer");
            flushAndClear();

            // when
            customTimerStepRepository.softDeleteAllByCustomTimer(timerWithNoSteps);

            // then
            List<CustomTimerStep> steps = customTimerStepRepository.findAll();
            assertThat(steps).isEmpty();
        }
    }
}