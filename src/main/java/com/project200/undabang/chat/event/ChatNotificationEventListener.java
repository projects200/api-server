package com.project200.undabang.chat.event;

import com.project200.undabang.chat.dto.event.ChatMessageCreatedEvent;
import com.project200.undabang.notification.fcm.service.ChatNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNotificationEventListener {

    private final ChatNotificationService chatNotificationService;

    @Async("generalPurposeAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatMessageSent(ChatMessageCreatedEvent event) {
        try {
            chatNotificationService.sendChatNotification(event);
        } catch (Exception e) {
            log.error("채팅 알림 발송 실패 chatId = {}", event.chatId(), e);
        }
    }
}
