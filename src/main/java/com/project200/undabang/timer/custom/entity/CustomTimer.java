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
    private LocalDateTime customTimerCreatedAt = LocalDateTime.now();

    @Builder.Default
    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "custom_timer_updated_at", nullable = false)
    private LocalDateTime customTimerUpdatedAt = LocalDateTime.now();

    @Column(name = "custom_timer_deleted_at")
    private LocalDateTime customTimerDeletedAt;

    @Builder.Default
    @OneToMany(mappedBy = "customTimer")
    private List<CustomTimerStep> customTimerSteps = new ArrayList<>();
}
