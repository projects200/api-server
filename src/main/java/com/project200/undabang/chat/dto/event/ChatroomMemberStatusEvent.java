package com.project200.undabang.chat.dto.event;

import com.project200.undabang.chat.dto.response.SaveMessageResponse;

public record ChatroomMemberStatusEvent(Long chatroomId, String chatContent) {
    public static ChatroomMemberStatusEvent of(Long chatroomId, String chatContent) {
        return new ChatroomMemberStatusEvent(chatroomId, chatContent);
    }
}
