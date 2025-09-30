package com.project200.undabang.openchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetOtherMemberOpenChatUrlResponse {
    private String openChatroomUrl;

    public static GetOtherMemberOpenChatUrlResponse of(String openChatroomUrl) {
        return new GetOtherMemberOpenChatUrlResponse(openChatroomUrl);
    }
}
