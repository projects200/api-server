package com.project200.undabang.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class GetMemberChatResponse {
    private List<ChatMessageDto> content;
    private boolean hasNext;
    private boolean opponentActive;

    public static GetMemberChatResponse from(Slice<ChatMessageDto> content, boolean isOpponentActive) {
        return GetMemberChatResponse.builder()
                .content(content.getContent())
                .hasNext(content.hasNext())
                .opponentActive(isOpponentActive)
                .build();
    }
}
