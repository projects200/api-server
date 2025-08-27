package com.project200.undabang.notification.fcm.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "fcm_tokens", uniqueConstraints = {
        @UniqueConstraint(name = "uk_fcm_token_value", columnNames = {"fcm_token_value"})
})
public class FcmToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("AUTO_INCREMENT")
    @Column(name = "fcm_token_id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Comment("UUID_SELF")
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Size(max = 255)
    @NotNull
    @Comment("FCM 토큰 값, unique")
    @Column(name = "fcm_token_value", nullable = false)
    private String fcmTokenValue;

    @Size(max = 255)
    @Comment("디바이스 정보 (User Agent)")
    @Column(name = "fcm_token_user_agent")
    private String fcmTokenUserAgent;

    @Builder.Default
    @NotNull
    @Comment("토큰 활성화 여부")
    @ColumnDefault("1")
    @Column(name = "fcm_token_is_active", nullable = false)
    private Boolean fcmTokenIsActive = true;

    @Builder.Default
    @NotNull
    @Comment("마지막 활성 일시")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fcm_token_activated_at", nullable = false)
    private LocalDateTime fcmTokenActivatedAt = LocalDateTime.now();

    @Builder.Default
    @NotNull
    @Comment("토큰 만료 일시 (활성화된 경우 현재 시점으로부터 30일 후)")
    @Column(name = "fcm_token_expired_at", nullable = false)
    private LocalDateTime fcmTokenExpiredAt = LocalDateTime.now().plusDays(30);

    @Builder.Default
    @NotNull
    @Comment("생성일시")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fcm_token_created_at", nullable = false)
    private LocalDateTime fcmTokenCreatedAt = LocalDateTime.now();

    public void activate() {
        this.fcmTokenIsActive = true;
        this.fcmTokenActivatedAt = LocalDateTime.now();
        this.fcmTokenExpiredAt = LocalDateTime.now().plusDays(30);
    }

    public void deactivate() {
        this.fcmTokenIsActive = false;
    }

    public boolean isActive() {
        return this.fcmTokenIsActive;
    }
}