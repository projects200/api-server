package com.project200.undabang.member.repository.impl;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
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
class MemberRepositoryImplTest {
    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    // Helper Methods
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

    private Exercise createAndSaveExercise(Member member, LocalDateTime startedAt) {
        Exercise exercise = Exercise.builder()
                .member(member)
                .exerciseTitle("testTitle")
                .exerciseStartedAt(startedAt)
                .exerciseEndedAt(startedAt.plusHours(1))
                .build();
        em.persist(exercise);
        return exercise;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("countMemberExerciseInLastDays 메소드는")
    class Describe_countMemberExerciseInLastDays {

        @Test
        @DisplayName("주어진 기간 내의 삭제되지 않은 운동 기록 개수를 정확히 반환한다")
        void it_returns_correct_count_of_non_deleted_exercises_within_period() {
            // given
            Member testMember = createAndSaveMember("testUser");
            Member anotherMember = createAndSaveMember("anotherUser");

            // 기간 내 운동 기록
            createAndSaveExercise(testMember, LocalDateTime.now()); // 오늘
            createAndSaveExercise(testMember, LocalDateTime.now().minusDays(3)); // 3일 전
            createAndSaveExercise(testMember, LocalDateTime.now().minusDays(6)); // 6일 전

            // 기간 밖 운동 기록
            createAndSaveExercise(testMember, LocalDateTime.now().minusDays(8)); // 8일 전

            // 삭제된 운동 기록
            Exercise deletedExercise = createAndSaveExercise(testMember, LocalDateTime.now().minusDays(1));
            deletedExercise.deleteExercise();
            em.persist(deletedExercise);

            // 다른 사용자의 운동 기록
            createAndSaveExercise(anotherMember, LocalDateTime.now().minusDays(2));

            flushAndClear();

            // when
            Long exerciseCount = memberRepository.countMemberExerciseInLastDays(testMember.getMemberId(), 7);

            // then
            assertThat(exerciseCount).isEqualTo(3);
        }

        @Test
        @DisplayName("기간 내에 운동 기록이 없으면 0을 반환한다")
        void it_returns_zero_when_no_exercises_in_period() {
            // given
            Member testMember = createAndSaveMember("testUser");
            createAndSaveExercise(testMember, LocalDateTime.now().minusDays(10));
            flushAndClear();

            // when
            Long exerciseCount = memberRepository.countMemberExerciseInLastDays(testMember.getMemberId(), 7);

            // then
            assertThat(exerciseCount).isEqualTo(0);
        }

        @Test
        @DisplayName("운동 기록이 전혀 없으면 0을 반환한다")
        void it_returns_zero_when_no_exercises_at_all() {
            // given
            Member testMember = createAndSaveMember("testUser");
            flushAndClear();

            // when
            Long exerciseCount = memberRepository.countMemberExerciseInLastDays(testMember.getMemberId(), 30);

            // then
            assertThat(exerciseCount).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("countMemberExerciseInThisYear 메소드는")
    class Describe_countMemberExerciseInThisYear {

        @Test
        @DisplayName("올해 수행한 운동 일수(중복 제외)를 정확히 반환한다")
        void it_returns_correct_distinct_day_count_in_this_year() {
            // given
            Member testMember = createAndSaveMember("testUser");
            Member anotherMember = createAndSaveMember("anotherUser");
            LocalDateTime now = LocalDateTime.now();

            // 올해 운동 기록 (중복된 날짜 포함)
            createAndSaveExercise(testMember, now); // 오늘
            createAndSaveExercise(testMember, now.minusHours(1)); // 오늘
            createAndSaveExercise(testMember, now.minusDays(1)); // 어제

            // 작년 운동 기록
            createAndSaveExercise(testMember, now.minusYears(1));

            // 삭제된 운동 기록
            Exercise deletedExercise = createAndSaveExercise(testMember, now.minusDays(2));
            deletedExercise.deleteExercise();
            em.persist(deletedExercise);

            // 다른 사용자의 운동 기록
            createAndSaveExercise(anotherMember, now);

            flushAndClear();

            // when
            Long exerciseDays = memberRepository.countMemberExerciseInThisYear(testMember.getMemberId());

            // then
            // 오늘, 어제 -> 2일
            assertThat(exerciseDays).isEqualTo(2);
        }

        @Test
        @DisplayName("올해 운동 기록이 없으면 0을 반환한다")
        void it_returns_zero_when_no_exercises_in_this_year() {
            // given
            Member testMember = createAndSaveMember("testUser");
            createAndSaveExercise(testMember, LocalDateTime.now().minusYears(1));
            flushAndClear();

            // when
            Long exerciseDays = memberRepository.countMemberExerciseInThisYear(testMember.getMemberId());

            // then
            assertThat(exerciseDays).isEqualTo(0);
        }

        @Test
        @DisplayName("운동 기록이 전혀 없으면 0을 반환한다")
        void it_returns_zero_when_no_exercises_at_all() {
            // given
            Member testMember = createAndSaveMember("testUser");
            flushAndClear();

            // when
            Long exerciseDays = memberRepository.countMemberExerciseInThisYear(testMember.getMemberId());

            // then
            assertThat(exerciseDays).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findAllByIdWithPessimisticLock 메소드는")
    class Describe_findAllByIdWithPessimisticLock {

        @Test
        @DisplayName("주어진 회원 ID 목록에 해당하는 회원들을 비관적 잠금과 함께 조회한다")
        void it_returns_members_with_pessimistic_lock() {
            // given
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");
            Member member3 = createAndSaveMember("user3");

            List<UUID> memberIds = List.of(
                    member1.getMemberId(),
                    member2.getMemberId(),
                    member3.getMemberId()
            );

            flushAndClear();

            // when
            List<Member> foundMembers = memberRepository.findAllByIdWithPessimisticLock(memberIds);

            // then
            assertThat(foundMembers).hasSize(3);
            assertThat(foundMembers).extracting("memberId")
                    .containsExactlyInAnyOrderElementsOf(memberIds);
        }

        @Test
        @DisplayName("빈 ID 목록을 전달하면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_empty_ids_provided() {
            // given
            List<UUID> emptyIds = List.of();

            // when
            List<Member> foundMembers = memberRepository.findAllByIdWithPessimisticLock(emptyIds);

            // then
            assertThat(foundMembers).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 ID는 결과에서 제외된다")
        void it_excludes_non_existent_ids() {
            // given
            Member existingMember = createAndSaveMember("existingUser");
            UUID nonExistentId = UUID.randomUUID();

            List<UUID> memberIds = List.of(
                    existingMember.getMemberId(),
                    nonExistentId
            );

            flushAndClear();

            // when
            List<Member> foundMembers = memberRepository.findAllByIdWithPessimisticLock(memberIds);

            // then
            assertThat(foundMembers).hasSize(1);
            assertThat(foundMembers.get(0).getMemberId()).isEqualTo(existingMember.getMemberId());
        }

        @Test
        @DisplayName("ID 목록 순서와 상관없이 모든 회원을 조회한다")
        void it_returns_all_members_regardless_of_id_order() {
            // given
            Member member1 = createAndSaveMember("user1");
            Member member2 = createAndSaveMember("user2");

            // 역순으로 ID 목록 생성
            List<UUID> memberIds = List.of(
                    member2.getMemberId(),
                    member1.getMemberId()
            );

            flushAndClear();

            // when
            List<Member> foundMembers = memberRepository.findAllByIdWithPessimisticLock(memberIds);

            // then
            assertThat(foundMembers).hasSize(2);
            assertThat(foundMembers).extracting("memberId")
                    .containsExactlyInAnyOrderElementsOf(memberIds);
        }
    }
}