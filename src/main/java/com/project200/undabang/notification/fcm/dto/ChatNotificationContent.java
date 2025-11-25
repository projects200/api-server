package com.project200.undabang.notification.fcm.dto;

import java.util.UUID;

public record ChatNotificationContent(UUID memberId, String memberNickname, Long chatroomId, String chatContent) {
}
