package com.project200.undabang.member.entity;

import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "preferred_exercises")
public class PreferredExercise {
    @Id
    @Column(name = "preferred_exercise_id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseType exercise;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "preferred_exercise_created_at", nullable = false)
    private LocalDateTime preferredExerciseCreatedAt = LocalDateTime.now();

    @Column(name = "preferred_exercise_deleted_at")
    private LocalDateTime preferredExerciseDeletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_exercise_skill_level", length = 30)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private ExerciseSkillLevel preferredExerciseSkillLevel;
}