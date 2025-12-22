package com.project200.undabang.chat.dto.request;

import com.project200.undabang.common.web.response.WebSocketType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private WebSocketType webSocketType;

    @Size(max = 500, message = "메시지는 500자를 초과할 수 없습니다.")
    private String content;
}
