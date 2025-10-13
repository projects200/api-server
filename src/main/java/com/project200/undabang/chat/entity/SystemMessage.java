package com.project200.undabang.chat.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 메시지를 표현하는 열거형 클래스.
 * 시스템 메시지는 채팅 방에서 발생하는 특정 이벤트를 알리기 위한 목적으로 사용된다.
 * 각 메시지는 고유한 코드와 메시지 템플릿을 포함한다.
 */
@Getter
@RequiredArgsConstructor
public enum SystemMessage {
    USER_CREATED_CHAT_ROOM("USER_CREATED_CHAT_ROOM", "%s 님이 채팅방에 입장하였습니다."),
    USER_LEFT_CHAT_ROOM("USER_LEFT_CHAT_ROOM", "%s 님이 채팅방을 나갔습니다.");

    private final String code;
    private final String template;

    public String format(String nickname) {
        return String.format(template, nickname);
    }
}
