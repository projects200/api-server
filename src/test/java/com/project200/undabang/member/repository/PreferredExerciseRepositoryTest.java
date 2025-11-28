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
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("test@email.com")
                .memberNickname("테스트유저")
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
            PreferredExercise deletedExercise = createPreferredExercise(member, exerciseType1);
            // delete() 메서드가 없다면 deletedAt 필드를 직접 설정하거나 soft delete 메서드를 확인해야 함.
            // 여기서는 deletedAt을 설정하는 setter가 없으므로, 엔티티에 delete 메서드가 없다고 가정하고
            // 엔티티를 수정하거나, 테스트 방식을 변경해야 함.
            // 하지만 기존 코드에서 delete()를 호출했으므로, 엔티티에 delete()가 있어야 함.
            // 에러 메시지: cannot find symbol method delete(LocalDateTime)
            // PreferredExercise 엔티티를 확인하지 못했으므로, 안전하게 리플렉션이나 다른 방법으로 삭제 처리하거나
            // delete 메서드가 없으면 삭제 테스트를 제외해야 함.
            // 일단 delete 메서드가 없어서 에러가 난 것으로 보이므로, 삭제 테스트 부분을 주석 처리하거나 수정해야 함.
            // PreferredExercise 엔티티에 delete 메서드가 없는 것 같음.
            // 대신 BaseEntity를 상속받았다면 deleteAt 필드가 있을 수 있음.
            // 여기서는 삭제 테스트를 위해 deletedExercise를 persist하지 않거나,
            // 삭제 로직을 검증하는 것이 목적이므로, 삭제된 상태로 저장해야 함.
            // 만약 delete 메서드가 없다면, 이 테스트 케이스는 수정이 필요함.
            // 일단 삭제 테스트를 제외하고 진행하거나, 엔티티를 수정해야 함.
            // 사용자가 엔티티 수정을 요청하지 않았으므로, 테스트 코드에서 삭제 로직을 제거하고
            // 삭제된 데이터가 조회되지 않는지 테스트하는 부분은 일단 보류하거나,
            // 삭제된 데이터를 생성하는 다른 방법을 찾아야 함.
            // 하지만 에러를 해결하는 것이 우선이므로, delete 호출을 제거하고
            // 대신 삭제된 상태로 생성하는 방법을 사용하거나 (빌더에 있다면),
            // 해당 테스트 케이스를 수정해야 함.

            // PreferredExercise 엔티티에 delete 메서드가 없으므로,
            // 삭제 테스트를 위해 리플렉션을 사용하여 deletedAt을 설정하거나,
            // 테스트를 수정해야 함. 여기서는 리플렉션을 사용하여 deletedAt을 설정함.
            org.springframework.test.util.ReflectionTestUtils.setField(deletedExercise, "preferredExerciseDeletedAt",
                    LocalDateTime.now());
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
}
