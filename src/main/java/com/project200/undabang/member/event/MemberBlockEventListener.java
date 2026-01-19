package com.project200.undabang.member.event;

import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.common.web.response.WebSocketResponse;
import com.project200.undabang.common.websocket.handler.WebSocketHandler;
import com.project200.undabang.member.dto.event.MemberBlockedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberBlockEventListener {

    private final WebSocketHandler webSocketHandler;
    private final ChatroomRepository chatroomRepository;

    @Async("generalPurposeAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberBlocked(MemberBlockedEvent event) {
        try {
            chatroomRepository.findChatroomBetweenMembers(event.blocked(), event.blocker())
                    .ifPresent(chatroom -> {
                        WebSocketResponse response = WebSocketResponse.system();
                        webSocketHandler.broadCastToAllChatroom(chatroom.getId(), response);
                    });
        } catch (Exception e) {
            log.error("채팅방 상태 변경 시스템 메시지 전송 실패.", e);
        }
    }
}
