package com.project200.undabang.openchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOpenChatRoomResponse {
    private Long openChatroomId;

    public static CreateOpenChatRoomResponse of(Long openChatroomId) {
        return new CreateOpenChatRoomResponse(openChatroomId);
    }
}
