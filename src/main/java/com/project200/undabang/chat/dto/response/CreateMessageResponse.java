package com.project200.undabang.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageResponse {
    private Long chatId;

    public static CreateMessageResponse of(Long id) {
        return new CreateMessageResponse(id);
    }
}
