package com.project200.undabang.notification.fcm.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    CHAT_MESSAGE("채팅 메시지 알림"),
    WORKOUT_REMINDER("운동 격려 알림");

    private final String description;
}
