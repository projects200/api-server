package com.project200.undabang.timer.simple.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.timer.simple.entity.SimpleTimer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class SimpleTimerRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private SimpleTimerRepository simpleTimerRepository;

    @Nested
    @DisplayName("findByMemberAndSimpleTimerDeletedAtNullOrderBySimpleTimerTimeAsc 메소드는")
    class Describe_findByMemberAndSimpleTimerDeletedAtNullOrderBySimpleTimerTimeAsc {

        @Test
        @DisplayName("특정 회원이 가지고 있는 삭제되지 않은 심플 타이머 목록을 시간 오름차순으로 조회한다")
        void it_returns_timers_for_the_given_member_ordered_by_time_asc() {
            // given
            Member member1 = createAndSaveMember("testMember1");
            Member member2 = createAndSaveMember("testMember2");

            SimpleTimer timer4 = createAndSaveSimpleTimer(member1, 60);
            SimpleTimer timer2 = createAndSaveSimpleTimer(member1, 40);
            SimpleTimer timer1 = createAndSaveSimpleTimer(member1, 30);
            SimpleTimer timer3 = createAndSaveSimpleTimer(member1, 50);
            createAndSaveSimpleTimer(member2, 300); // 다른 회원의 타이머

            flushAndClear();

            // when
            List<SimpleTimer> foundTimers = simpleTimerRepository.findByMemberAndSimpleTimerDeletedAtNullOrderBySimpleTimerTimeAsc(member1);

            // then
            assertThat(foundTimers).hasSize(4);
            assertThat(foundTimers).extracting(SimpleTimer::getId)
                    .containsExactly(timer1.getId(), timer2.getId(), timer3.getId(), timer4.getId());
        }

        @Test
        @DisplayName("삭제된 타이머는 조회 결과에서 제외한다")
        void it_excludes_deleted_timers() {
            // given
            Member member = createAndSaveMember("testMember");

            SimpleTimer activeTimer = createAndSaveSimpleTimer(member, 30);
            SimpleTimer deletedTimer = createAndSaveSimpleTimer(member, 60);
            deletedTimer.deleteSimpleTimer(); // 수정된 부분
            em.persist(deletedTimer);

            flushAndClear();

            // when
            List<SimpleTimer> foundTimers = simpleTimerRepository.findByMemberAndSimpleTimerDeletedAtNullOrderBySimpleTimerTimeAsc(member);

            // then
            assertThat(foundTimers).hasSize(1);
            assertThat(foundTimers.get(0).getId()).isEqualTo(activeTimer.getId());
        }

        @Test
        @DisplayName("심플 타이머가 없는 회원을 조회하면 빈 리스트를 반환한다")
        void it_returns_an_empty_list_when_no_timers_exist() {
            // given
            Member member = createAndSaveMember("testMember");
            flushAndClear();

            // when
            List<SimpleTimer> foundTimers = simpleTimerRepository.findByMemberAndSimpleTimerDeletedAtNullOrderBySimpleTimerTimeAsc(member);

            // then
            assertThat(foundTimers).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdAndMemberAndSimpleTimerDeletedAtNull 메소드는")
    class Describe_findByIdAndMemberAndSimpleTimerDeletedAtNull {

        @Test
        @DisplayName("ID와 회원 정보가 일치하는 삭제되지 않은 타이머를 조회한다")
        void it_returns_a_timer_for_the_given_id_and_member() {
            // given
            Member member = createAndSaveMember("testMember");
            SimpleTimer timer = createAndSaveSimpleTimer(member, 180);
            flushAndClear();

            // when
            Optional<SimpleTimer> foundTimerOpt = simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(timer.getId(), member);

            // then
            assertThat(foundTimerOpt).isPresent();
            assertThat(foundTimerOpt.get().getId()).isEqualTo(timer.getId());
            assertThat(foundTimerOpt.get().getMember().getMemberId()).isEqualTo(member.getMemberId()); // 수정된 부분
        }


        @Test
        @DisplayName("타이머는 존재하지만 다른 회원의 소유일 경우 빈 Optional을 반환한다")
        void it_returns_empty_optional_if_timer_belongs_to_another_member() {
            // given
            Member ownerMember = createAndSaveMember("ownerMember");
            Member otherMember = createAndSaveMember("otherMember");
            SimpleTimer timer = createAndSaveSimpleTimer(ownerMember, 180);
            flushAndClear();

            // when
            Optional<SimpleTimer> foundTimerOpt = simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(timer.getId(), otherMember);

            // then
            assertThat(foundTimerOpt).isNotPresent();
        }

        @Test
        @DisplayName("존재하지 않는 타이머 ID로 조회할 경우 빈 Optional을 반환한다")
        void it_returns_empty_optional_for_non_existent_timer_id() {
            // given
            Member member = createAndSaveMember("testMember");
            Long nonExistentTimerId = 999L;
            flushAndClear();

            // when
            Optional<SimpleTimer> foundTimerOpt = simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(nonExistentTimerId, member);

            // then
            assertThat(foundTimerOpt).isNotPresent();
        }

        @Test
        @DisplayName("삭제된 타이머 ID로 조회할 경우 빈 Optional을 반환한다")
        void it_returns_empty_optional_for_deleted_timer() {
            // given
            Member member = createAndSaveMember("testMember");
            SimpleTimer timer = createAndSaveSimpleTimer(member, 180);
            timer.deleteSimpleTimer(); // 수정된 부분
            em.persist(timer);
            flushAndClear();

            // when
            Optional<SimpleTimer> foundTimerOpt = simpleTimerRepository.findByIdAndMemberAndSimpleTimerDeletedAtNull(timer.getId(), member);

            // then
            assertThat(foundTimerOpt).isNotPresent();
        }
    }

    private Member createAndSaveMember(String nickname) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@email.com")
                .memberNickname(nickname)
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(2000,1,1))
                .build();
        em.persist(member);

        return member;
    }

    private SimpleTimer createAndSaveSimpleTimer(Member member, int time) {
        SimpleTimer timer = SimpleTimer.builder()
                .member(member)
                .simpleTimerTime(time)
                .build();
        em.persist(timer);
        return timer;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}