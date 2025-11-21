package com.project200.undabang.notification.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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

    @NotNull
    @Column(name = "notification_type_code", nullable = false, length = 50)
    private String notificationTypeCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type_category", nullable = false, length = 50)
    private NotificationCategory category;

    @Builder.Default
    @NotNull
    @Column(name = "notification_type_default_enabled", nullable = false)
    private Boolean defaultEnabled = true;

    @Builder.Default
    @NotNull
    @Column(name = "notification_type_is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @NotNull
    @Column(name = "notification_type_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}