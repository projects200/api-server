package com.project200.undabang.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatroomResponse {

    private Long chatRoomId;

    public static CreateChatroomResponse of(Long id) {
        return new CreateChatroomResponse(id);
    }
}
