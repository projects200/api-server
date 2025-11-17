package com.project200.undabang.notification.fcm.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "fcm_tokens")
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
    @Column(name = "fcm_token_value", nullable = false, unique = true)
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

    @Builder.Default
    @OneToMany(mappedBy = "fcmToken", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DeviceNotificationSetting> deviceNotificationSettingList = new ArrayList<>();

    public static FcmToken from(Member member, String fcmTokenValue, String userAgent) {
        return FcmToken.builder()
                .member(member)
                .fcmTokenValue(fcmTokenValue)
                .fcmTokenUserAgent(userAgent)
                .build();
    }

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

    public void updateOwner(Member member, String userAgent) {
        this.member = member;
        this.fcmTokenUserAgent = userAgent;
        this.activate();
    }
}