package com.project200.undabang.notification.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "notification_types", uniqueConstraints = {
        @UniqueConstraint(name = "uq_notification_type_code", columnNames = "notification_type_code")
})
public class NotificationType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_type_id")
    private Long id;

    @Comment("알림 타입을 식별하는 코드 (예: CHAT_MESSAGE, WORKOUT_REMINDER)")
    @NotNull
    @Column(name = "notification_type_code", nullable = false, length = 50)
    private String notificationTypeCode;

    @Comment("알림 카테고리 (PERSONAL, NOTICE)")
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type_category", nullable = false, length = 50)
    private NotificationCategory category;

    @Comment("신규 사용자에게 기본적으로 활성화되는지 여부")
    @Builder.Default
    @NotNull
    @Column(name = "notification_type_default_enabled", nullable = false)
    private Boolean defaultEnabled = true;

    @Comment("해당 알림 타입의 활성화 여부")
    @Builder.Default
    @NotNull
    @Column(name = "notification_type_is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @NotNull
    @Column(name = "notification_type_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}