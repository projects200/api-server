package com.project200.undabang.chat.service;

import java.util.UUID;

public interface ChatUpdateService {
    void updateLastReadChatId(Long chatId, UUID memberId, Long lastReadChatId);
}
