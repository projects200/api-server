package com.project200.undabang.openchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetOpenChatUrlResponse {
    private String openChatroomUrl;

    public static GetOpenChatUrlResponse of(String openChatroomUrl) {
        return new GetOpenChatUrlResponse(openChatroomUrl);
    }
}