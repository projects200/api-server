package com.project200.undabang.member.entity;

import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "preferred_exercises")
public class PreferredExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preferred_exercise_id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false, updatable = false)
    private ExerciseType exercise;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "preferred_exercise_created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime preferredExerciseCreatedAt = LocalDateTime.now();

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "preferred_exercise_updated_at")
    @Builder.Default
    private LocalDateTime preferredExerciseUpdatedAt = LocalDateTime.now();

    @NotNull
    @ColumnDefault("0")
    @Column(name = "preferred_exercise_date", nullable = false)
    @Builder.Default
    private Byte preferredExerciseDate = (byte) 0;

    @Column(name = "preferred_exercise_deleted_at")
    private LocalDateTime preferredExerciseDeletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_exercise_skill_level", length = 30)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private ExerciseSkillLevel preferredExerciseSkillLevel;

    /**
     * 선호하는 운동 요일을 나타내는 boolean 배열을 반환합니다.
     * 각 배열의 값은 월요일부터 일요일까지의 요일을 나타내며,
     * 특정 요일에 선호 운동이 설정되어 있는 경우 true를 반환합니다.
     *
     * @return boolean 배열로, 월요일부터 일요일까지의 각 요일에 대한 선호 여부를 나타냅니다.
     */
    public boolean[] getDaysOfWeek() {
        boolean[] days = new boolean[7];
        for (int i = 0; i < 7; i++) {
            days[i] = (preferredExerciseDate & (1 << i)) != 0;
        }
        return days;
    }

    /**
     * 선호하는 운동 요일을 설정합니다.
     * 주어진 boolean 배열에서 각 인덱스는 월요일부터 일요일까지의 요일을 나타냅니다.
     * 배열 값이 true인 경우 해당 요일에 선호 운동이 설정됩니다.
     *
     * @param days 월요일부터 일요일까지의 요일을 나타내는 boolean 배열
     *             (예: 0번 인덱스는 월요일, 6번 인덱스는 일요일을 나타냄)
     */
    public void setDaysOfWeek(boolean[] days) {
        byte date = 0;
        for (int i = 0; i < 7; i++) {
            if (days[i]) {
                date |= (byte) (1 << i);
            }
        }
        this.preferredExerciseDate = date;
    }

    public static PreferredExercise createPreferredExercise(Member member, ExerciseType exercise,
            ExerciseSkillLevel skillLevel, boolean[] daysOfWeek) {
        PreferredExercise preferredExercise = PreferredExercise.builder()
                .member(member)
                .exercise(exercise)
                .preferredExerciseSkillLevel(skillLevel)
                .build();

        preferredExercise.setDaysOfWeek(daysOfWeek);

        return preferredExercise;
    }

    public void update(ExerciseSkillLevel skillLevel, boolean[] daysOfWeek) {
        this.preferredExerciseSkillLevel = skillLevel;
        setDaysOfWeek(daysOfWeek);
        this.preferredExerciseUpdatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.preferredExerciseDeletedAt = LocalDateTime.now();
    }
}