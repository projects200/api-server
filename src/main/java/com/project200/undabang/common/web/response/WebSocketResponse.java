package com.project200.undabang.common.web.response;

import lombok.Getter;

@Getter
public class WebSocketResponse<T> {
    private final WebSocketType webSocketType;
    private final T data;

    private WebSocketResponse(WebSocketType webSocketType, T data) {
        this.webSocketType = webSocketType;
        this.data = data;
    }

    public static <T> WebSocketResponse<T> success(T data) {
        return new WebSocketResponse<>(WebSocketType.TALK, data);
    }

    public static <T> WebSocketResponse<T> heartbeat() {
        return new WebSocketResponse<>(WebSocketType.PONG, null);
    }

    public static <T> WebSocketResponse<T> heartbeat(T data) {
        return new WebSocketResponse<>(WebSocketType.PONG, data);
    }

    public static <T> WebSocketResponse<T> error(T data) {
        return new WebSocketResponse<>(WebSocketType.ERROR, data);
    }
}
