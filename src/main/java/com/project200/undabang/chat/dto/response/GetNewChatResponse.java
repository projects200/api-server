package com.project200.undabang.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetNewChatResponse {
    private List<ChatMessageDto> newChats;
    private boolean opponentActive;
    private boolean blockActive;

    public static GetNewChatResponse of(List<ChatMessageDto> newChats, boolean isOpponentActive, boolean blockActive) {
        return GetNewChatResponse.builder()
                .newChats(newChats)
                .opponentActive(isOpponentActive)
                .blockActive(blockActive)
                .build();
    }
}
