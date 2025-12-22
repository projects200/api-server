package com.project200.undabang.member.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import com.project200.undabang.member.enums.MemberGender;
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
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class PreferredExerciseRepositoryTest {

    @Autowired
    PreferredExerciseRepository preferredExerciseRepository;

    @Autowired
    EntityManager em;

    // ============== 테스트 헬퍼 메소드 ==============

    private void persist(Object entity) {
        em.persist(entity);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private Member createMember() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("test" + uniqueId + "@email.com")
                .memberNickname("user" + uniqueId)
                .memberGender(MemberGender.MALE)
                .memberScore((byte) 35)
                .memberBday(LocalDate.of(1990, 1, 1))
                .build();
    }

    private ExerciseType createExerciseType(String name) {
        return ExerciseType.builder()
                .exerciseName(name)
                .exerciseTypeImageUrl("http://example.com/" + name + ".jpg")
                .build();
    }

    private PreferredExercise createPreferredExercise(Member member, ExerciseType exerciseType) {
        PreferredExercise preferredExercise = PreferredExercise.builder()
                .member(member)
                .exercise(exerciseType)
                .preferredExerciseSkillLevel(ExerciseSkillLevel.INTERMEDIATE)
                .build();

        preferredExercise.setDaysOfWeek(new boolean[] { true, false, true, false, true, false, false });
        return preferredExercise;
    }

    @Nested
    @DisplayName("findAllByMemberAndPreferredExerciseDeletedAtNull 메소드는")
    class FindAllByMemberAndPreferredExerciseDeletedAtNull {

        @Test
        @DisplayName("삭제되지 않은 회원의 선호 운동 목록을 조회한다")
        void findAllByMemberAndPreferredExerciseDeletedAtNull_Success() {
            // given
            Member member = createMember();
            persist(member);

            ExerciseType exerciseType1 = createExerciseType("헬스");
            persist(exerciseType1);

            ExerciseType exerciseType2 = createExerciseType("러닝");
            persist(exerciseType2);

            PreferredExercise preferredExercise1 = createPreferredExercise(member, exerciseType1);
            persist(preferredExercise1);

            PreferredExercise preferredExercise2 = createPreferredExercise(member, exerciseType2);
            persist(preferredExercise2);

            // 삭제된 데이터 (조회되지 않아야 함)
            PreferredExercise deletedExercise = PreferredExercise.builder()
                    .member(member)
                    .exercise(exerciseType1)
                    .preferredExerciseSkillLevel(ExerciseSkillLevel.INTERMEDIATE)
                    .preferredExerciseDeletedAt(LocalDateTime.now())
                    .build();
            deletedExercise.setDaysOfWeek(new boolean[] { true, false, true, false, true, false, false });
            persist(deletedExercise);

            flushAndClear();

            // when
            List<PreferredExercise> result = preferredExerciseRepository
                    .findAllByMemberAndPreferredExerciseDeletedAtNull(member);

            // then
            assertThat(result).hasSize(2);
            assertSoftly(softly -> {
                softly.assertThat(result).extracting("id").containsExactlyInAnyOrder(preferredExercise1.getId(),
                        preferredExercise2.getId());
                softly.assertThat(result).extracting("exercise.exerciseName").containsExactlyInAnyOrder("헬스", "러닝");
            });
        }

        @Test
        @DisplayName("선호 운동이 없는 경우 빈 리스트를 반환한다")
        void findAllByMemberAndPreferredExerciseDeletedAtNull_Empty() {
            // given
            Member member = createMember();
            persist(member);

            flushAndClear();

            // when
            List<PreferredExercise> result = preferredExerciseRepository
                    .findAllByMemberAndPreferredExerciseDeletedAtNull(member);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByIdInAndMemberAndPreferredExerciseDeletedAtNull 메소드는")
    class FindAllByIdInAndMemberAndPreferredExerciseDeletedAtNull {

        @Test
        @DisplayName("주어진 ID 목록과 회원에 해당하는 삭제되지 않은 선호 운동 목록을 조회한다")
        void findAllByIdInAndMemberAndPreferredExerciseDeletedAtNull_Success() {
            // given
            Member member = createMember();
            persist(member);

            ExerciseType exerciseType1 = createExerciseType("헬스");
            persist(exerciseType1);
            ExerciseType exerciseType2 = createExerciseType("러닝");
            persist(exerciseType2);
            ExerciseType exerciseType3 = createExerciseType("수영");
            persist(exerciseType3);

            PreferredExercise preferredExercise1 = createPreferredExercise(member, exerciseType1);
            persist(preferredExercise1);
            PreferredExercise preferredExercise2 = createPreferredExercise(member, exerciseType2);
            persist(preferredExercise2);
            PreferredExercise preferredExercise3 = createPreferredExercise(member, exerciseType3);
            persist(preferredExercise3);

            // 다른 회원의 데이터
            Member anotherMember = createMember();
            persist(anotherMember);
            PreferredExercise anotherMemberExercise = createPreferredExercise(anotherMember, exerciseType1);
            persist(anotherMemberExercise);

            flushAndClear();

            // when
            List<Long> targetIds = List.of(preferredExercise1.getId(), preferredExercise2.getId());
            List<PreferredExercise> result = preferredExerciseRepository
                    .findAllByIdInAndMemberAndPreferredExerciseDeletedAtNull(targetIds, member);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting("id").containsExactlyInAnyOrder(preferredExercise1.getId(),
                    preferredExercise2.getId());
        }
    }
}
