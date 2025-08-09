package com.project200.undabang.timer.simple.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "simple_timer_order")
    private Integer simpleTimerOrder;

    @Column(name = "simple_timer_time")
    private Integer simpleTimerTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder.Default
    @Column(name = "simple_timer_created_at", nullable = false, updatable = false)
    private LocalDateTime simpleTimerCreatedAt = LocalDateTime.now();

    @Column(name = "simple_timer_updated_at")
    private LocalDateTime simpleTimerUpdatedAt;

    @Column(name = "simple_timer_deleted_at")
    private LocalDateTime simpleTimerDeletedAt;
}