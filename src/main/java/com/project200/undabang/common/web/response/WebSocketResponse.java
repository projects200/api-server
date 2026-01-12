package com.project200.undabang.common.web.response;

import com.project200.undabang.common.web.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketResponse<T> {
    private boolean succeed;
    private WebSocketType type;
    private String message;
    private T data;

    public static <T> WebSocketResponse<T> success(T data) {
        return new WebSocketResponse<>(true, WebSocketType.TALK, null, data);
    }

    public static <T> WebSocketResponse<T> success(WebSocketType type, T data) {
        return new WebSocketResponse<>(true, type, null, data);
    }

    public static <T> WebSocketResponse<T> heartbeat() {
        return new WebSocketResponse<>(true, WebSocketType.PONG, null, null);
    }

    public static WebSocketResponse<String> error(String message) {
        return new WebSocketResponse<>(false, WebSocketType.ERROR, null, message);
    }

    public static WebSocketResponse<String> error(WebSocketType type, ErrorCode errorCode) {
        return new WebSocketResponse<>(false, type, errorCode.getCode(), errorCode.getMessage());
    }
}
