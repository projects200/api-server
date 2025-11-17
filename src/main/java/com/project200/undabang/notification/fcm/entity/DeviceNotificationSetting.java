package com.project200.undabang.notification.fcm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "device_notification_settings", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_token_and_type",
                columnNames = {"fcm_token_id", "notification_type"}
        )
})
public class DeviceNotificationSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("설정 테이블의 고유 식별자")
    @Column(name = "setting_id", nullable = false)
    private Long id;

    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE) // JPA를 통하지 않는 DB 직접 삭제 등에서도 정합성을 보장하기 위한 DB 레벨의 이중 안전장치
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Comment("설정의 주체가 되는 디바이스의 ID")
    @JoinColumn(name = "fcm_token_id", nullable = false)
    private FcmToken fcmToken;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Comment("알림 종류를 나타내는 코드 (예: CHAT_MESSAGE, WORKOUT_REMINDER)")
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Builder.Default
    @NotNull
    @Comment("해당 알림 수신 여부 (TRUE: 켬, FALSE: 끔)")
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Comment("설정이 마지막으로 변경된 시간")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @NotNull
    @Comment("설정 레코드가 처음 생성된 시간")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static DeviceNotificationSetting of(FcmToken fcmToken, NotificationType notificationType) {
        return DeviceNotificationSetting.builder()
                .fcmToken(fcmToken)
                .notificationType(notificationType)
                .isEnabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
}