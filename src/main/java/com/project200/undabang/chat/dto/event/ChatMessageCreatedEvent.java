package com.project200.undabang.chat.dto.event;

import com.project200.undabang.chat.entity.Chat;
import lombok.Builder;

import java.util.UUID;

/**
 * ChatMessageCreatedEvent 클래스는 채팅 메시지가 생성될 때 발생하는 이벤트를 나타냅니다.
 * 이 이벤트는 채팅 ID, 채팅방 ID, 발신자 ID를 포함하여 메시지 생성과 관련된 정보를 제공합니다.
 * <p>
 * Immutable한 데이터 전달을 위해 Java Record를 사용하며, Builder 패턴을 통해 객체를 생성할 수 있습니다.
 * <p>
 * 메서드:
 * - from(Chat chat): 주어진 Chat 엔티티를 기반으로 ChatMessageCreatedEvent 객체를 생성합니다.
 */
@Builder
public record ChatMessageCreatedEvent(Long chatId, Long chatroomId, UUID senderId) {
    public static ChatMessageCreatedEvent from(Chat chat) {
        return ChatMessageCreatedEvent.builder()
                .chatId(chat.getId())
                .chatroomId(chat.getChatroom().getId())
                .senderId(chat.getSender().getMemberId())
                .build();
    }
}
