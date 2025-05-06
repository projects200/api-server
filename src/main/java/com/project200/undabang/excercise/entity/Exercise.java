package com.project200.undabang.excercise.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "exercises")
public class Exercise {
    @Id
    @Column(name = "exercise_id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "exercise_started_at", nullable = false)
    private LocalDateTime exerciseStartedAt = LocalDateTime.now();

    @NotNull
    @ColumnDefault("((`exercise_started_at` + interval 1 hour))")
    @Column(name = "exercise_ended_at", nullable = false)
    private LocalDateTime exerciseEndedAt = LocalDateTime.now().plusHours(1);

    @NotNull
    @Lob
    @Column(name = "exercise_detail", nullable = false, columnDefinition = "text")
    private String exerciseDetail;

    @Size(max = 255)
    @NotNull
    @Column(name = "exercise_title", nullable = false)
    private String exerciseTitle;

    @Size(max = 255)
    @NotNull
    @org.hibernate.annotations.Comment("시스템이 아닌 개인 등록")
    @Column(name = "exercise_personal_type", nullable = false)
    private String exercisePersonalType;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "exercise_created_at", nullable = false)
    private LocalDateTime exerciseCreatedAt = LocalDateTime.now();

    @Column(name = "exercise_deleted_at")
    private LocalDateTime exerciseDeletedAt;

}