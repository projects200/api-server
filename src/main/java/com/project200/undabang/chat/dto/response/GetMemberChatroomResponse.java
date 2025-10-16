package com.project200.undabang.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetMemberChatroomResponse {
    private Long chatRoomId;
    private String otherMemberNickname;
    private String otherMemberProfileImageUrl;
    private String otherMemberThumbnailImageUrl;
    private String lastChatContent;
    private LocalDateTime lastChatReceivedAt;
    private Long unreadCount;
}
