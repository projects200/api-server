package com.project200.undabang.chat.event;

import com.project200.undabang.chat.dto.event.ChatMessageCreatedEvent;
import com.project200.undabang.notification.fcm.service.ChatNotificationService;
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

import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class ChatNotificationEventListenerTest {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private ChatNotificationService chatNotificationService;

    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "generalPurposeAsyncExecutor") // 실제 코드의 Executor 빈 이름과 일치시켜야 함
        @Primary
        public Executor generalPurposeAsyncExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Nested
    @DisplayName("handleChatMessageSent 메소드는")
    class HandleChatMessageSentTest {

        @BeforeEach
        void setUp() {
            reset(chatNotificationService);
        }

        @Test
        @DisplayName("채팅 저장 트랜잭션 커밋 후 이벤트가 처리되어 알림 발송 서비스가 호출된다")
        void afterTransactionCommit() {
            // given
            Long chatId = 100L;
            Long chatroomId = 1L;
            UUID senderId = UUID.randomUUID();
            ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(chatId, chatroomId, senderId);

            // when
            // 트랜잭션 내에서 이벤트 발행 -> 커밋 -> 리스너 실행
            transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(event);
                return null;
            });

            // then
            // 서비스 메소드가 정확히 1번 호출되었는지 검증
            verify(chatNotificationService, times(1)).sendChatNotification(event);
        }

        @Test
        @DisplayName("트랜잭션이 롤백되면 리스너가 실행되지 않는다")
        void rollbackTransaction() {
            // given
            ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(1L, 1L, UUID.randomUUID());

            // when
            try {
                transactionTemplate.execute(status -> {
                    eventPublisher.publishEvent(event);
                    status.setRollbackOnly(); // 강제 롤백 설정
                    return null;
                });
            } catch (Exception ignored) {
            }

            // then
            // 커밋되지 않았으므로 리스너는 실행되지 않아야 함
            verify(chatNotificationService, never()).sendChatNotification(any());
        }

        @Test
        @DisplayName("알림 발송 중 예외가 발생해도 로그만 남기고 안전하게 종료된다")
        void handlesExceptionGracefully() {
            // given
            ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(1L, 1L, UUID.randomUUID());

            // 서비스가 예외를 던지도록 설정
            doThrow(new RuntimeException("FCM 서버 오류")).when(chatNotificationService).sendChatNotification(event);

            // when & then
            // 예외가 외부(메인 스레드)로 전파되지 않는지 검증
            assertDoesNotThrow(() -> transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(event);
                return null;
            }));

            // 예외가 났더라도 호출 자체는 시도되었어야 함
            verify(chatNotificationService, times(1)).sendChatNotification(event);
        }
    }

    @Nested
    @DisplayName("비동기 처리 검증")
    class AsyncExecutionTest {

        @Test
        @DisplayName("테스트 환경에서는 SyncTaskExecutor에 의해 동기적으로(메인 스레드에서) 실행된다")
        void isExecutedSynchronouslyInTest() {
            // given
            ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(1L, 1L, UUID.randomUUID());
            final String mainThreadName = Thread.currentThread().getName();
            final String[] listenerThreadName = {null};

            // 서비스가 호출될 때, 실행 중인 스레드 이름을 캡처하도록 설정
            doAnswer(invocation -> {
                listenerThreadName[0] = Thread.currentThread().getName();
                return null;
            }).when(chatNotificationService).sendChatNotification(event);

            // when
            transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(event);
                return null;
            });

            // then
            // TestConfig 덕분에 메인 스레드와 리스너 스레드가 같아야 함
            assertThat(listenerThreadName[0]).isEqualTo(mainThreadName);
        }
    }
}