package com.project200.undabang.timer.custom.entity;

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
@Table(name = "custom_timer_steps")
public class CustomTimerStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_timer_steps_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_timer_id", nullable = false)
    private CustomTimer customTimer;

    @Comment("스텝 이름")
    @Column(name = "custom_timer_steps_name", length = 50)
    private String customTimerStepName;

    @Comment("스텝 순서")
    @NotNull
    @Column(name = "custom_timer_steps_order", nullable = false)
    private Byte customTimerStepOrder;

    @Comment("스텝 시간")
    @NotNull
    @Column(name = "custom_timer_steps_time", nullable = false)
    private Integer customTimerStepTime;

    @Builder.Default
    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "custom_timer_steps_created_at", nullable = false, updatable = false)
    private LocalDateTime customTimerStepCreatedAt = LocalDateTime.now();

    @Builder.Default
    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "custom_timer_steps_updated_at", nullable = false)
    private LocalDateTime customTimerStepUpdatedAt = LocalDateTime.now();

    @Column(name = "custom_timer_steps_deleted_at")
    private LocalDateTime customTimerStepDeletedAt;
}