package com.project200.undabang.timer.custom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "custom_timer_steps")
public class CustomTimerStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_timer_steps_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_timer_id", nullable = false)
    private CustomTimer customTimer;

    @Column(name = "custom_timer_steps_name", length = 50)
    private String name;

    @Column(name = "custom_timer_steps_order", nullable = false)
    private Integer order;

    @Column(name = "custom_timer_steps_time", nullable = false)
    private Integer time;

    @Builder.Default
    @Column(name = "custom_timer_steps_created_at", nullable = false, updatable = false)
    private LocalDateTime customTimerStepCreatedAt = LocalDateTime.now();

    @Column(name = "custom_timer_steps_updated_at")
    private LocalDateTime customTimerStepUpdatedAt;

    @Column(name = "custom_timer_steps_deleted_at")
    private LocalDateTime customTimerStepDeletedAt;
}