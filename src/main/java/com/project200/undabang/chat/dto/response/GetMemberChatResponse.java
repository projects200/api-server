package com.project200.undabang.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

/**
 * @param content        실제 데이터 리스트
 * @param hasNext        다음 페이지 존재 유무
 * @param opponentActive 상대방의 활성상태 조회
 */
@Builder
@AllArgsConstructor
public record GetMemberChatResponse(List<ChatMessageDto> content, boolean hasNext, boolean opponentActive) {
    public static GetMemberChatResponse from(Slice<ChatMessageDto> content, boolean isOpponentActive) {
        return GetMemberChatResponse.builder()
                .content(content.getContent())
                .hasNext(content.hasNext())
                .opponentActive(isOpponentActive)
                .build();
    }
}
