package com.project200.undabang.chat.dto.record;

import java.util.UUID;

public record SaveMessageRecord(long chatroomId, UUID memberId, String chatContent) {
    public static SaveMessageRecord of(long chatroomId, UUID memberId, String chatContent) {
        return new SaveMessageRecord(chatroomId, memberId, chatContent);
    }
}
