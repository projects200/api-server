package com.project200.undabang.chat.repository;

import com.project200.undabang.notification.fcm.dto.ChatNotificationContent;

import java.util.Optional;

public interface ChatRepositoryCustom {
    Optional<ChatNotificationContent> findChatContentForNotification(Long chatId);
}
