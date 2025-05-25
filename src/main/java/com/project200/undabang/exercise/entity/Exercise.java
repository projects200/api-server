package com.project200.undabang.exercise.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "exercises")
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @NotNull
//    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "exercise_started_at")
    @Builder.Default
    private LocalDateTime exerciseStartedAt = LocalDateTime.now();

    @NotNull
//    @ColumnDefault("((`exercise_started_at` + interval 1 hour))")
    // H2 DB 저장시 해당 Default값이 Error 생성
    @Column(name = "exercise_ended_at")
    @Builder.Default
    private LocalDateTime exerciseEndedAt = LocalDateTime.now().plusHours(1);

    @Lob
    @Column(name = "exercise_detail", columnDefinition = "text")
    private String exerciseDetail;

    @Size(max = 255)
    @NotNull
    @Column(name = "exercise_title", nullable = false)
    private String exerciseTitle;

    @Size(max = 255)
    @org.hibernate.annotations.Comment("시스템이 아닌 개인 등록")
    @Column(name = "exercise_personal_type")
    private String exercisePersonalType;

    @Size(max = 255)
    @Column(name = "exercise_location")
    private String exerciseLocation;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "exercise_created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime exerciseCreatedAt = LocalDateTime.now();

    @Column(name = "exercise_deleted_at")
    private LocalDateTime exerciseDeletedAt;

    public boolean isOwnedBy(UUID memberId) {
        return this.member.getMemberId().equals(memberId);
    }
}