package com.project200.undabang.common.web.response;

import com.project200.undabang.common.web.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * WebSocketResponse 클래스는 웹소켓 응답 데이터를 처리하기 위한 데이터 구조를 제공합니다.
 * 이 클래스는 제네릭 타입으로 구성되어 있어 다양한 데이터 타입을 포함할 수 있습니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketResponse<T> {
    private boolean succeed;
    private WebSocketType type;
    private String message;
    private T data;

    /**
     * 성공 상태의 WebSocketResponse 객체를 생성합니다.
     * 데이터와 함께 성공적인 응답을 나타내는 객체를 반환합니다.
     */
    public static <T> WebSocketResponse<T> success(T data) {
        return new WebSocketResponse<>(true, WebSocketType.TALK, null, data);
    }

    /**
     * 성공 상태의 WebSocketResponse 객체를 생성합니다.
     * 주어진 WebSocketType과 데이터를 기반으로 성공적인 응답을 나타내는 객체를 반환합니다.
     */
    public static <T> WebSocketResponse<T> success(WebSocketType type, T data) {
        return new WebSocketResponse<>(true, type, null, data);
    }

    /**
     * WebSocket 연결 상태를 유지하기 위한 heartbeat 응답을 생성합니다.
     * 이 메소드는 WebSocket 연결의 안정성과 지속적인 유지를 확인하기 위해 사용됩니다.
     */
    public static <T> WebSocketResponse<T> heartbeat() {
        return new WebSocketResponse<>(true, WebSocketType.PONG, null, null);
    }

    /**
     * 에러 상태의 WebSocketResponse 객체를 생성합니다.
     * 주어진 에러 메시지를 포함하여 실패 응답 객체를 반환합니다.
     */
    public static WebSocketResponse<String> error(String message) {
        return new WebSocketResponse<>(false, WebSocketType.ERROR, null, message);
    }

    /**
     * 주어진 WebSocketType과 ErrorCode를 기반으로 에러 상태를 나타내는 WebSocketResponse 객체를 생성합니다.
     * 이 메소드는 실패한 웹소켓 응답에 대한 정보를 전달하는 데 사용됩니다.
     */
    public static WebSocketResponse<String> error(WebSocketType type, ErrorCode errorCode) {
        return new WebSocketResponse<>(false, type, errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 시스템 메시지(WebSocketType.SYSTEM_LEAVE)를 나타내는 WebSocketResponse 객체를 생성합니다.
     * 이 메소드는 특정 시스템 이벤트(예: 채팅방 퇴장 알림)를 클라이언트에 전달하는 데 사용됩니다.
     */
    public static WebSocketResponse<Void> system(String content) {
        return new WebSocketResponse<>(true, WebSocketType.SYSTEM_LEAVE, content, null);
    }

    /**
     * 차단된 회원과의 대화 불가를 알리는 시스템 알림용 응답을 생성합니다. (성공 상태)
     * 회원 차단으로 인해 더 이상 메시지를 주고받을 수 없음을 알리는 시스템 메시지 전송에 사용합니다.
     * 데이터(data) 필드는 사용하지 않고, 메시지(message) 필드에 고정된 안내 문구를 담아 보냅니다.
     */
    public static WebSocketResponse<Void> system() {
        return new WebSocketResponse<>(true, WebSocketType.SYSTEM_BANNED, "차단된 회원과는 대화할 수 없습니다.", null);
    }
}
