package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.chat.dto.event.ChatMessageCreatedEvent;
import com.project200.undabang.chat.repository.ChatRepository;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.dto.ChatNotificationContent;
import com.project200.undabang.notification.fcm.dto.ChatNotificationPayload;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import com.project200.undabang.notification.fcm.service.ChatNotificationService;
import com.project200.undabang.notification.fcm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatNotificationServiceImpl implements ChatNotificationService {
    private final ChatRepository chatRepository;
    private final ChatroomMemberRepository chatroomMemberRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationService notificationService;

    /**
     * notificationService
     * 채팅 메시지 생성 이벤트를 기반으로 상대방에게 채팅 알림을 전송합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public void sendChatNotification(ChatMessageCreatedEvent event) {
        ChatNotificationContent content = chatRepository.findChatContentForNotification(event.chatId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));

        Member otherMember = chatroomMemberRepository.findOtherMemberInChatroom(event.chatroomId(), event.senderId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND));

        List<String> activatedFcmTokenListForChat = fcmTokenRepository.findAllActivatedFcmTokensForChat(otherMember.getMemberId());

        ChatNotificationPayload payload = ChatNotificationPayload.from(content);

        notificationService.sendChatNotification(payload, activatedFcmTokenListForChat);
    }
}
