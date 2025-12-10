package com.project200.undabang.notification.fcm.dto;

import com.project200.undabang.notification.entity.NotificationCode;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 채팅 메시지 데이터를 표현하는 DTO 클래스입니다.
 * <p>
 * 주어진 정보를 이용해 채팅 관련 데이터를 구조화하고,
 * 이를 적절한 형식의 Map 데이터로 변환할 수 있습니다.
 * <p>
 * 이 클래스는 채팅 데이터를 다른 시스템이나 컴포넌트와 상호 작용할 수 있도록
 * 필요한 포맷으로 변환하는 유틸리티 메서드를 제공합니다.
 */
@Getter
@Builder
public class ChatNotificationPayload {
    private String type;
    private Long chatroomId;
    private UUID memberId;
    private String nickname;
    private String content;

    /**
     * 주어진 ChatNotificationContent 객체를 바탕으로 ChatNotificationPayload 객체를 생성합니다.
     */
    public static ChatNotificationPayload from(ChatNotificationContent content) {
        return ChatNotificationPayload.builder()
                .type(NotificationCode.CHAT_MESSAGE.getCode())
                .chatroomId(content.chatroomId())
                .memberId(content.memberId())
                .nickname(content.memberNickname())
                .content(content.chatContent())
                .build();
    }

    /**
     * 현재 객체의 채팅 데이터를 키-값 쌍의 형식인 Map으로 변환합니다.
     * Data 타입의 FCM 알림은 Map<String, String> 형태만 허용하기 때문에 해당 메소드가 필요합니다.
     * 반환되는 Map은 채팅 데이터(type, chatroomId, memberId, nickname, content)를 포함합니다.
     */
    public Map<String, String> toChatData() {
        Map<String, String> data = new HashMap<>();
        data.put("type", type);
        data.put("chatroomId", String.valueOf(chatroomId));
        data.put("memberId", memberId.toString());
        data.put("nickname", nickname);
        data.put("content", content);
        return data;
    }
}