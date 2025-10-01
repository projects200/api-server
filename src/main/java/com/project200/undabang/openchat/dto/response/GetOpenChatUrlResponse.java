package com.project200.undabang.openchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetOpenChatUrlResponse {
    private String openChatroomUrl;

    public static GetOpenChatUrlResponse of(String openChatroomUrl) {
        return new GetOpenChatUrlResponse(openChatroomUrl);
    }
}