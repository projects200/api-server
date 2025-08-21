package com.project200.undabang.timer.custom.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class CustomTimerStepRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private CustomTimerStepRepository customTimerStepRepository;

    private Member createAndSaveMember() {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("test@email.com")
                .memberNickname("test")
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
        em.persist(member);
        return member;
    }

    private CustomTimer createAndSaveCustomTimer(Member member) {
        CustomTimer timer = CustomTimer.builder()
                .member(member)
                .customTimerName("timer")
                .build();
        em.persist(timer);
        return timer;
    }

    private CustomTimerStep createAndSaveStep(CustomTimer timer, String name, byte order) {
        CustomTimerStep step = CustomTimerStep.builder()
                .customTimer(timer)
                .customTimerStepName(name)
                .customTimerStepOrder(order)
                .customTimerStepTime(60)
                .build();
        em.persist(step);
        return step;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("findAllByCustomTimerAndCustomTimerStepDeletedAtNull 메소드는")
    class Describe_findAllByCustomTimerAndCustomTimerStepDeletedAtNull {

        @Test
        @DisplayName("특정 타이머의 삭제되지 않은 스텝 목록을 반환한다")
        void it_returns_active_steps() {
            // given
            Member member = createAndSaveMember();
            CustomTimer timer = createAndSaveCustomTimer(member);

            CustomTimerStep step1 = createAndSaveStep(timer, "step1", (byte) 1);
            CustomTimerStep step2 = createAndSaveStep(timer, "step2", (byte) 2);

            // 삭제된 스텝
            CustomTimerStep deletedStep = CustomTimerStep.builder()
                    .customTimer(timer)
                    .customTimerStepName("deleted")
                    .customTimerStepOrder((byte) 3)
                    .customTimerStepTime(30)
                    .customTimerStepDeletedAt(LocalDateTime.now())
                    .build();
            em.persist(deletedStep);

            flushAndClear();

            // when
            List<CustomTimerStep> foundSteps = customTimerStepRepository.findAllByCustomTimerAndCustomTimerStepDeletedAtNull(timer);

            // then
            Assertions.assertThat(foundSteps).hasSize(2);
            Assertions.assertThat(foundSteps).extracting(CustomTimerStep::getCustomTimerStepName)
                    .containsExactlyInAnyOrder("step1", "step2");
        }

        @Test
        @DisplayName("스텝이 없는 경우 빈 리스트를 반환한다")
        void it_returns_empty_list_when_no_steps() {
            // given
            Member member = createAndSaveMember();
            CustomTimer timer = createAndSaveCustomTimer(member);
            flushAndClear();

            // when
            List<CustomTimerStep> foundSteps = customTimerStepRepository.findAllByCustomTimerAndCustomTimerStepDeletedAtNull(timer);

            // then
            Assertions.assertThat(foundSteps).isNotNull().isEmpty();
        }
    }
}