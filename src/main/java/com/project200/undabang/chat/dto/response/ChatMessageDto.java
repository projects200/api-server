package com.project200.undabang.chat.dto.response;

import com.project200.undabang.chat.entity.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long chatId;
    private UUID senderId;
    private String senderNickname;
    private String senderProfileUrl;
    private String senderThumbnailUrl;
    private String chatContent;
    private ChatType chatType;
    private LocalDateTime sentAt;
    private boolean isMine;
}
