package com.project200.undabang.timer.custom.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.project200.undabang.timer.sync.TimerSyncService.currentTimestamp;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "custom_timers")
public class CustomTimer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_timer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Comment("커스텀 타이머 이름")
    @Column(name = "custom_timer_name", length = 100)
    private String customTimerName;

    @Builder.Default
    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "custom_timer_created_at", nullable = false, updatable = false)
    private LocalDateTime customTimerCreatedAt = currentTimestamp();

    @Builder.Default
    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "custom_timer_updated_at", nullable = false, columnDefinition = "datetime(3)")
    private LocalDateTime customTimerUpdatedAt = currentTimestamp();

    @Column(name = "custom_timer_deleted_at")
    private LocalDateTime customTimerDeletedAt;

    @Column(name = "edited_at")
    private Long editedAt;

    @Builder.Default
    @OneToMany(mappedBy = "customTimer")
    private List<CustomTimerStep> customTimerSteps = new ArrayList<>();

    public static CustomTimer of(Member member, String customTimerName, Long editedAt) {
        LocalDateTime now = currentTimestamp();
        return CustomTimer.builder()
                .member(member)
                .customTimerName(customTimerName)
                .customTimerCreatedAt(now)
                .customTimerUpdatedAt(now)
                .editedAt(editedAt)
                .build();
    }

    public void deleteCustomTimer(Long editedAt) {
        LocalDateTime now = currentTimestamp();
        this.customTimerDeletedAt = now;
        this.customTimerUpdatedAt = now;
        this.editedAt = editedAt;
    }

    public void updateCustomTimerName(String customTimerName, Long editedAt) {
        this.customTimerName = customTimerName;
        this.customTimerUpdatedAt = currentTimestamp();
        this.editedAt = editedAt;
    }
}
