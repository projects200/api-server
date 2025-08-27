package com.project200.undabang.notification.fcm.dto;

public record NotificationContent(
        String title,
        String body,
        String imageUrl
) {
}
