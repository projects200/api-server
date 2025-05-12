package com.project200.undabang.exercise.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "exercise_types")
public class ExerciseType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_id", nullable = false, updatable = false)
    private Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "exercise_name", nullable = false, length = 50)
    private String exerciseName;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "exercise_type_created_at", nullable = false, updatable = false)
    private LocalDateTime exerciseTypeCreatedAt = LocalDateTime.now();

    @Column(name = "exercise_type_deleted_at")
    private LocalDateTime exerciseTypeDeletedAt;

    @Size(max = 10)
    @NotNull
    @Column(name = "exercise_type_emoji", nullable = false, length = 10)
    private String exerciseTypeEmoji;

}