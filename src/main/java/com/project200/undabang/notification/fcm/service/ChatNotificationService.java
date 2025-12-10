package com.project200.undabang.notification.fcm.service;

import com.project200.undabang.chat.dto.event.ChatMessageCreatedEvent;

public interface ChatNotificationService {
    void sendChatNotification(ChatMessageCreatedEvent event);
}
