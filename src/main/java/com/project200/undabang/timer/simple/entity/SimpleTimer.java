package com.project200.undabang.timer.simple.entity;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.timer.sync.TimerSyncService;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.Objects;

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
    private LocalDateTime simpleTimerCreatedAt = TimerSyncService.currentTimestamp();

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(3)")
    @Builder.Default
    @Column(name = "simple_timer_updated_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime simpleTimerUpdatedAt = TimerSyncService.currentTimestamp();

    @Column(name = "simple_timer_deleted_at")
    private LocalDateTime simpleTimerDeletedAt;

    @Column(name = "edited_at")
    private Long editedAt;

    public static SimpleTimer of(Member member, Integer time, Long editedAt) {
        LocalDateTime now = TimerSyncService.currentTimestamp();
        return SimpleTimer.builder()
                .member(member)
                .simpleTimerTime(time)
                .simpleTimerCreatedAt(now)
                .simpleTimerUpdatedAt(now)
                .editedAt(editedAt == null ? System.currentTimeMillis() : editedAt)
                .build();
    }

    public void updateSimpleTimer(Integer time, Long editedAt) {
        if (editedAt != null || !Objects.equals(this.simpleTimerTime, time)) {
            this.simpleTimerTime = time;
            this.simpleTimerUpdatedAt = TimerSyncService.currentTimestamp();
            this.editedAt = editedAt == null ? System.currentTimeMillis() : editedAt;
        }
    }

    public void deleteSimpleTimer(Long editedAt) {
        LocalDateTime now = TimerSyncService.currentTimestamp();
        this.simpleTimerDeletedAt = now;
        this.simpleTimerUpdatedAt = now;
        this.editedAt = editedAt == null ? System.currentTimeMillis() : editedAt;
    }
}