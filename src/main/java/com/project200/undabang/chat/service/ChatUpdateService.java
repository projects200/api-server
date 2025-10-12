package com.project200.undabang.chat.service;

import com.project200.undabang.member.entity.Member;

public interface ChatUpdateService {
    void updateLastReadChatId(Long chatId, Member member, Long lastReadChatId);
}
