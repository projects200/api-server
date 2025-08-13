package com.project200.undabang.member.event;

import com.project200.undabang.member.dto.event.MemberSignedUpEvent;
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class MemberEventListenerTest {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private SimpleTimerCommandService simpleTimerCommandService;

    // 테스트 시 @Async를 동기적으로 실행하기 위한 설정
    @TestConfiguration
    static class AsyncTestConfig {
        @Bean
        @Primary
        public Executor generalPurposeAsyncExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Nested
    @DisplayName("handleMemberSignedUp 메소드는")
    class HandleMemberSignedUpTest {
        // 테스트간 독립성을 위해 사용
        @BeforeEach
        void setUp() {
            reset(simpleTimerCommandService);
        }

        @Test
        @DisplayName("회원가입 트랜잭션 커밋 후 이벤트가 처리되어 타이머 생성 메소드가 호출된다")
        void afterTransactionCommit() {
            // given
            UUID memberId = UUID.randomUUID();
            MemberSignedUpEvent event = new MemberSignedUpEvent(memberId); // 회원가입 이벤트 생성

            // when
            // 내부 코드를 하나의 트랜잭션으로 실행하고, 블록이 끝나면 커밋
            transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(event); // 이벤트 발행
                return null;
            });

            // then
            verify(simpleTimerCommandService, times(1)).createDefaultSimpleTimer(memberId);
        }

        @Test
        @DisplayName("이벤트의 memberId가 null일 경우 타이머 생성 메소드가 호출되지 않는다")
        void withNullEvent() {
            // given
            // memberId가 null인 이벤트를 생성합니다.
            // 이벤트 리스너가 이벤트 내부의 null 값을 올바르게 처리하는지 검증하기 위함입니다.
            MemberSignedUpEvent eventWithNullMemberId = new MemberSignedUpEvent(null);

            // when
            transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(eventWithNullMemberId);
                return null;
            });

            // then
            verify(simpleTimerCommandService, never()).createDefaultSimpleTimer(any());
        }

        @Test
        @DisplayName("타이머 생성 중 예외 발생 시 로그만 기록되고 예외가 전파되지 않는다")
        void handlesExceptionGracefully() {
            // given
            UUID memberId = UUID.randomUUID();
            MemberSignedUpEvent event = new MemberSignedUpEvent(memberId);
            doThrow(new RuntimeException("DB 저장 실패")).when(simpleTimerCommandService).createDefaultSimpleTimer(memberId);

            // when & then
            // 람다 내부의 코드 실행 시 어떤 에러도 발생하지 않음 (catch 에서 처리)
            assertDoesNotThrow(() -> transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(event);
                return null;
            }));

            verify(simpleTimerCommandService, times(1)).createDefaultSimpleTimer(memberId);
        }
    }

    @Nested
    @DisplayName("비동기 처리 설정은")
    class AsyncConfigurationTest {
        @BeforeEach
        void setUp() {
            reset(simpleTimerCommandService);
        }

        @Test
        @DisplayName("테스트 환경에서 동기적으로 실행된다")
        void isExecutedSynchronouslyInTest() {
            // given
            UUID memberId = UUID.randomUUID();
            MemberSignedUpEvent event = new MemberSignedUpEvent(memberId);
            final String mainThreadName = Thread.currentThread().getName(); // 메인 쓰레드 이름
            final String[] asyncThreadName = {null}; // 비동기 쓰레드 이름

            doAnswer(invocation -> { // Mock 객체의 메소드가 호출될 때 실행될 동작 정의
                asyncThreadName[0] = Thread.currentThread().getName(); // 비동기 메소드를 실행하는 쓰레드의 이름을 저장
                return null;
            }).when(simpleTimerCommandService).createDefaultSimpleTimer(memberId);

            // when
            // 이벤트 발행하여 비동기 메소드 호출
            transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(event);
                return null;
            });

            // then
            // 테스트 메인 쓰레드와 비동기 쓰레드의 이름이 같은지 확인 (같으면 비동기 처리가 동기적으로 실행됨)
            assertThat(asyncThreadName[0]).isEqualTo(mainThreadName);
        }
    }
}