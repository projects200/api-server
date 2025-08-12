package com.project200.undabang.timer.simple.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "simple_timers")
public class SimpleTimer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "simple_timer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Comment("심플 타이머 시간")
    @Column(name = "simple_timer_time")
    private Integer simpleTimerTime;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Builder.Default
    @Column(name = "simple_timer_created_at", nullable = false, updatable = false)
    private LocalDateTime simpleTimerCreatedAt = LocalDateTime.now();

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Builder.Default
    @Column(name = "simple_timer_updated_at", nullable = false)
    private LocalDateTime simpleTimerUpdatedAt = LocalDateTime.now();

    @Column(name = "simple_timer_deleted_at")
    private LocalDateTime simpleTimerDeletedAt;
}