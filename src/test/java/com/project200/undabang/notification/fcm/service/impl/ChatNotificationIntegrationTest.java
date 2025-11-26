package com.project200.undabang.notification.fcm.service.impl;

import com.project200.undabang.chat.dto.request.CreateMessageRequest;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.chat.service.ChatCommandService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.notification.entity.NotificationCategory;
import com.project200.undabang.notification.entity.NotificationCode;
import com.project200.undabang.notification.entity.NotificationType;
import com.project200.undabang.notification.fcm.dto.ChatNotificationPayload;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
import com.project200.undabang.notification.fcm.service.NotificationService;
import com.project200.undabang.notification.repository.NotificationTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class ChatNotificationIntegrationTest {

    @Autowired
    private ChatCommandService chatCommandService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatroomRepository chatroomRepository;

    @Autowired
    private ChatroomMemberRepository chatroomMemberRepository;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private FcmTokenCommandService fcmTokenCommandService;

    @Autowired
    private NotificationTypeRepository notificationTypeRepository;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("통합: 채팅 메시지 전송 시 비동기 이벤트 흐름을 타고 알림이 발송된다")
    void sendChatMessage_ShouldTriggerNotificationFlow() {
        // given
        persistNotificationType(NotificationCode.CHAT_MESSAGE.getCode());

        // 1. 회원 및 채팅방 데이터 준비
        Member sender = persistMember("sender");
        Member receiver = persistMember("receiver");

        Chatroom chatroom = persistChatroom();
        persistChatroomMember(chatroom, sender);
        persistChatroomMember(chatroom, receiver);

        // 2. 수신자 FCM 토큰 등록 (이제 NotificationType이 있으므로 설정도 같이 생김)
        fcmTokenCommandService.saveFcmToken(receiver, "token_receiver_123", "ios");

        CreateMessageRequest request = new CreateMessageRequest("통합 테스트 메시지");

        try (MockedStatic<UserContextHolder> utilities = Mockito.mockStatic(UserContextHolder.class)) {
            utilities.when(UserContextHolder::getUserId).thenReturn(sender.getMemberId());

            // when
            chatCommandService.createMessage(chatroom.getId(), request);
        }

        // then
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            ArgumentCaptor<ChatNotificationPayload> payloadCaptor = ArgumentCaptor.forClass(ChatNotificationPayload.class);
            ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);

            // 호출 여부 검증
            verify(notificationService).sendChatNotification(payloadCaptor.capture(), tokensCaptor.capture());

            // 토큰 리스트 검증 (이제 1개가 되어야 함)
            assertThat(tokensCaptor.getValue())
                    .hasSize(1)
                    .contains("token_receiver_123");
        });
    }

    private void persistNotificationType(String code) {
        NotificationType type = NotificationType.builder()
                .notificationTypeCode(code)
                .category(NotificationCategory.PERSONAL)
                .defaultEnabled(true)
                .isActive(true)
                .build();

        notificationTypeRepository.save(type);
    }

    private Member createMember(String nickname) {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname(nickname)
                .memberEmail(nickname + "@test.com")
                .build();
    }

    private Member persistMember(String nickname) {
        return memberRepository.save(createMember(nickname));
    }

    private Chatroom persistChatroom() {
        return chatroomRepository.save(Chatroom.createChatroom());
    }

    private void persistChatroomMember(Chatroom chatroom, Member member) {
        chatroomMemberRepository.save(ChatroomMember.of(chatroom, member));
    }
}
