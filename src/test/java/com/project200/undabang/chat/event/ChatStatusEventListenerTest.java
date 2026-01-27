package com.project200.undabang.chat.event;

import com.project200.undabang.chat.dto.event.ChatroomMemberStatusEvent;
import com.project200.undabang.common.web.response.WebSocketType;
import com.project200.undabang.common.websocket.handler.WebSocketHandler;
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

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class ChatStatusEventListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private WebSocketHandler webSocketHandler;

    @BeforeEach
    void setUp() {
        reset(webSocketHandler);
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
    @DisplayName("handleChatroomDeleted 메소드는")
    class HandleChatroomDeletedTest {

        @Test
        @DisplayName("트랜잭션 커밋 후 채팅방 상태 변경 시스템 메시지를 전송한다")
        void afterTransactionCommit() {
            // given
            Long chatroomId = 1L;
            String content = "채팅방이 삭제되었습니다.";
            ChatroomMemberStatusEvent event = new ChatroomMemberStatusEvent(chatroomId, content);

            // when
            transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(event);
                return null;
            });

            // then
            verify(webSocketHandler, times(1))
                    .broadCastToAllChatroom(eq(chatroomId), argThat(response ->
                            response.getType() == WebSocketType.SYSTEM_LEAVE // 혹은 로직에 맞는 타입 확인
                                    && response.getMessage().equals(content)
                    ));
        }

        @Test
        @DisplayName("트랜잭션이 롤백되면 알림을 전송하지 않는다")
        void rollbackTransaction() {
            // given
            ChatroomMemberStatusEvent event = new ChatroomMemberStatusEvent(1L, "롤백 테스트");

            // when
            try {
                transactionTemplate.execute(status -> {
                    eventPublisher.publishEvent(event);
                    status.setRollbackOnly(); // 롤백 유도
                    return null;
                });
            } catch (Exception ignored) {
            }

            // then
            verify(webSocketHandler, never()).broadCastToAllChatroom(any(), any());
        }

        @Test
        @DisplayName("전송 중 예외가 발생해도 로그를 남기고 종료된다")
        void handlesExceptionGracefully() {
            // given
            ChatroomMemberStatusEvent event = new ChatroomMemberStatusEvent(1L, "예외 테스트");

            doThrow(new RuntimeException("Socket Error"))
                    .when(webSocketHandler).broadCastToAllChatroom(any(), any());

            // when & then
            assertDoesNotThrow(() -> transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(event);
                return null;
            }));

            verify(webSocketHandler, times(1)).broadCastToAllChatroom(any(), any());
        }
    }
}