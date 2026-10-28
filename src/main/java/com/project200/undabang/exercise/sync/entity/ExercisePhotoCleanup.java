package com.project200.undabang.exercise.sync.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "exercise_photo_cleanup", indexes = @Index(name = "idx_exercise_cleanup_due", columnList = "next_attempt_at"))
public class ExercisePhotoCleanup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_photo_cleanup_id")
    private Long id;

    @Column(name = "object_key", nullable = false, updatable = false, length = 512, unique = true)
    private String objectKey;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    public static ExercisePhotoCleanup of(String objectKey) {
        ExercisePhotoCleanup task = new ExercisePhotoCleanup();
        task.objectKey = objectKey;
        task.nextAttemptAt = LocalDateTime.now();
        return task;
    }

    public void failed() {
        attempts++;
        nextAttemptAt = LocalDateTime.now().plusMinutes(5);
    }
}
