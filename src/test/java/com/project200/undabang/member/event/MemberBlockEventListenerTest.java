package com.project200.undabang.member.event;

import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.common.web.response.WebSocketType;
import com.project200.undabang.common.websocket.handler.WebSocketHandler;
import com.project200.undabang.member.dto.event.MemberBlockedEvent;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class MemberBlockEventListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private WebSocketHandler webSocketHandler;

    @MockitoBean
    private ChatroomRepository chatroomRepository;

    @BeforeEach
    void setUp() {
        reset(webSocketHandler, chatroomRepository);
    }

    private Member createMember(UUID memberId, String nickname) {
        return Member.builder()
                .memberId(memberId)
                .memberNickname(nickname)
                .memberEmail(nickname + "@test.com")
                .memberGender(MemberGender.MALE)
                .memberBday(LocalDate.of(1995, 1, 1))
                .memberScore((byte) 36)
                .memberWarnedCount((byte) 0)
                .build();
    }

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "generalPurposeAsyncExecutor")
        @Primary
        public Executor generalPurposeAsyncExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Nested
    @DisplayName("handleMemberBlocked 메소드는")
    class HandleMemberBlockedTest {

        @Test
        @DisplayName("트랜잭션 커밋 후 두 회원 사이의 채팅방이 존재하면 차단 알림(SYSTEM_BANNED)을 전송한다")
        void sendNotificationWhenChatroomExists() {
            // given
            // 1. Member 객체 생성
            Member blocker = createMember(UUID.randomUUID(), "blockerUser");
            Member blocked = createMember(UUID.randomUUID(), "blockedUser");

            // 2. 이벤트 생성 (ID가 아니라 Member 객체를 직접 주입)
            MemberBlockedEvent event = MemberBlockedEvent.of(blocked, blocker);

            // 3. 채팅방 Mock 설정
            Long existingChatroomId = 100L;
            Chatroom mockChatroom = mock(Chatroom.class);
            when(mockChatroom.getId()).thenReturn(existingChatroomId);

            // 4. 레포지토리 Mocking (인자가 Member 객체임)
            when(chatroomRepository.findChatroomBetweenMembers(blocked, blocker))
                    .thenReturn(Optional.of(mockChatroom));

            // when
            transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(event);
                return null;
            });

            // then
            verify(chatroomRepository, times(1)).findChatroomBetweenMembers(blocked, blocker);

            verify(webSocketHandler, times(1))
                    .broadCastToAllChatroom(eq(existingChatroomId), argThat(response ->
                            response.getType() == WebSocketType.SYSTEM_BANNED
                    ));
        }

        @Test
        @DisplayName("채팅방이 존재하지 않으면(Optional.empty) 알림을 보내지 않고 종료한다")
        void doNothingWhenChatroomNotFound() {
            // given
            Member blocker = createMember(UUID.randomUUID(), "blockerUser");
            Member blocked = createMember(UUID.randomUUID(), "blockedUser");
            MemberBlockedEvent event = MemberBlockedEvent.of(blocked, blocker);

            // 채팅방 없음 설정
            when(chatroomRepository.findChatroomBetweenMembers(blocked, blocker))
                    .thenReturn(Optional.empty());

            // when
            transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(event);
                return null;
            });

            // then
            verify(chatroomRepository, times(1)).findChatroomBetweenMembers(any(Member.class), any(Member.class));
            verify(webSocketHandler, never()).broadCastToAllChatroom(any(), any());
        }

        @Test
        @DisplayName("트랜잭션이 롤백되면 리스너가 실행되지 않는다")
        void rollbackTransaction() {
            // given
            Member blocker = createMember(UUID.randomUUID(), "blocker");
            Member blocked = createMember(UUID.randomUUID(), "blocked");
            MemberBlockedEvent event = MemberBlockedEvent.of(blocked, blocker);

            // when
            try {
                transactionTemplate.execute(status -> {
                    eventPublisher.publishEvent(event);
                    status.setRollbackOnly(); // 강제 롤백
                    return null;
                });
            } catch (Exception ignored) {
            }

            // then
            verify(chatroomRepository, never()).findChatroomBetweenMembers(any(), any());
            verify(webSocketHandler, never()).broadCastToAllChatroom(any(), any());
        }

        @Test
        @DisplayName("알림 전송 중 예외가 발생해도 로그를 남기고 안전하게 종료된다")
        void handlesExceptionGracefully() {
            // given
            Member blocker = createMember(UUID.randomUUID(), "blockerUser");
            Member blocked = createMember(UUID.randomUUID(), "blockedUser");
            MemberBlockedEvent event = MemberBlockedEvent.of(blocked, blocker);

            Chatroom mockChatroom = mock(Chatroom.class);
            when(mockChatroom.getId()).thenReturn(1L);

            when(chatroomRepository.findChatroomBetweenMembers(blocked, blocker))
                    .thenReturn(Optional.of(mockChatroom));

            // WebSocketHandler가 예외를 던지도록 설정
            doThrow(new RuntimeException("WebSocket Error"))
                    .when(webSocketHandler).broadCastToAllChatroom(any(), any());

            // when & then
            assertDoesNotThrow(() -> transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(event);
                return null;
            }));

            // 예외가 발생했어도 호출 시도는 1번 했어야 함
            verify(webSocketHandler, times(1)).broadCastToAllChatroom(any(), any());
        }
    }
}