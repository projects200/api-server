package com.project200.undabang.timer.custom.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import jakarta.persistence.EntityManager;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class CustomTimerRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private CustomTimerRepository customTimerRepository;

    @Autowired
    private MemberRepository memberRepository;

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
        CustomTimer timer = CustomTimer.builder()
                .member(member)
                .customTimerName(name)
                .build();
        em.persist(timer);
        return timer;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("findByMemberAndCustomTimerDeletedAtNull 메소드는")
    class Describe_findByMemberAndCustomTimerDeletedAtNull {

        @Test
        @DisplayName("특정 회원이 가지고 있는 삭제되지 않은 커스텀 타이머 목록을 조회한다")
        void it_returns_timers_for_the_given_member() {
            // given
            Member member1 = createAndSaveMember("testMember1");
            Member member2 = createAndSaveMember("testMember2");

            CustomTimer timer1 = createAndSaveCustomTimer(member1, "timer1");
            CustomTimer timer2 = createAndSaveCustomTimer(member1, "timer2");
            createAndSaveCustomTimer(member2, "timer3"); // 다른 회원의 타이머

            flushAndClear();

            // when
            List<CustomTimer> foundTimers = customTimerRepository.findAllByMemberAndCustomTimerDeletedAtNull(member1);

            // then
            assertThat(foundTimers).hasSize(2);
            assertThat(foundTimers).extracting(CustomTimer::getId)
                    .containsExactlyInAnyOrder(timer1.getId(), timer2.getId());
        }

        @Test
        @DisplayName("커스텀 타이머가 없는 회원을 조회하면 빈 리스트를 반환한다")
        void it_returns_an_empty_list_when_no_timers_exist() {
            // given
            Member member = createAndSaveMember("testMember");
            flushAndClear();

            // when
            List<CustomTimer> foundTimers = customTimerRepository.findAllByMemberAndCustomTimerDeletedAtNull(member);

            // then
            assertThat(foundTimers).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("삭제된 타이머는 조회되지 않는다")
        void it_does_not_return_deleted_timers() {
            // given
            Member member = createAndSaveMember("testMember");
            CustomTimer activeTimer = createAndSaveCustomTimer(member, "active timer");
            CustomTimer deletedTimer = CustomTimer.builder()
                    .member(member)
                    .customTimerName("deleted timer")
                    .customTimerDeletedAt(LocalDateTime.now())
                    .build();
            em.persist(deletedTimer);
            flushAndClear();

            // when
            List<CustomTimer> foundTimers = customTimerRepository.findAllByMemberAndCustomTimerDeletedAtNull(member);

            // then
            assertThat(foundTimers).hasSize(1);
            assertThat(foundTimers.get(0).getId()).isEqualTo(activeTimer.getId());
        }
    }
}