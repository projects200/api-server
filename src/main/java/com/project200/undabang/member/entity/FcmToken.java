package com.project200.undabang.member.entity;

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

    @NotNull
    @Comment("마지막 활성 일시")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fcm_token_activated_at", nullable = false)
    private LocalDateTime fcmTokenActivatedAt;

    @NotNull
    @Comment("생성일시")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fcm_token_created_at", nullable = false)
    private LocalDateTime fcmTokenCreatedAt;

    @Comment("삭제일시")
    @Column(name = "fcm_token_deleted_at")
    private LocalDateTime fcmTokenDeletedAt;

}