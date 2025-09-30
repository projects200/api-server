package com.project200.undabang.openchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOpenChatRoomResponse {
    private Long openChatroomId;

    public static UpdateOpenChatRoomResponse of(Long openChatroomId) {
        return new UpdateOpenChatRoomResponse(openChatroomId);
    }
}
