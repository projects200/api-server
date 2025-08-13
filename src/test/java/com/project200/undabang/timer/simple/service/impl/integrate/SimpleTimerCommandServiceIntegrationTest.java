package com.project200.undabang.timer.simple.service.impl.integrate;

import com.project200.undabang.member.dto.event.MemberSignedUpEvent;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.simple.repository.SimpleTimerRepository;
import com.project200.undabang.timer.simple.service.impl.SimpleTimerCommandServiceImpl;
import org.assertj.core.api.Assertions;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// Bean 재정의 허용(테스트 설정의 비동기 설정 빈을 사용해서 메인 어플리케이션의 빈을 덮어쓰도록 허용. 빈 중복정의 방지)
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
public class SimpleTimerCommandServiceIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private MemberRepository memberRepository;
    @MockitoBean
    private SimpleTimerRepository simpleTimerRepository;
    @MockitoBean
    private PolicyService policyService;
    @Autowired
    private SimpleTimerCommandServiceImpl simpleTimerCommandService;
    @Autowired
    private TransactionTemplate transactionTemplate; // 트랜잭션을 프로그래밍 방식으로 제어, 트랜잭션 커밋 시점에 발생하는 이벤트 테스트시 사용

    private void setupDefaultPolicyMock() {
        given(policyService.getPolicyValueAsInt(any())).willReturn(6);
        given(policyService.getPolicyValueAsString(any())).willReturn("30,40,50,60,75,90");
    }

    private Member createAndSaveTestMember() {
        // 테스트 격리를 위해 TransactionTemplate 사용
        return transactionTemplate.execute(status -> memberRepository.save(Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("test-" + UUID.randomUUID() + "@example.com")
                .memberNickname("testuser-" + UUID.randomUUID())
                .build()));
    }

    // 테스트 시 @Async를 동기적으로 실행하기 위한 설정
    @TestConfiguration
    static class AsyncTestConfig {
        @Bean(name = "generalPurposeAsyncExecutor")
        @Primary // 테스트 시 이 Executor를 우선적으로 사용하도록 설정
        public SyncTaskExecutor generalPurposeAsyncExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Nested
    @DisplayName("트랜잭션 이벤트 리스너 동작")
    class TransactionalEventTest {
        @BeforeEach
        void setUp() {
            reset(simpleTimerRepository);
        }

        @Test
        @DisplayName("회원가입 트랜잭션 커밋 후 이벤트가 처리되어 타이머가 생성된다")
        void createDefaultSimpleTimer_afterTransactionCommit() {
            // given
            setupDefaultPolicyMock();
            Member testMember = createAndSaveTestMember();

            // when
            // TransactionTemplate을 사용해 트랜잭션을 명시적으로 커밋
            // 내부에서 event 발행. execute 블록이 끝나면 트랜잭션이 커밋됨
            transactionTemplate.execute(status -> {
                eventPublisher.publishEvent(new MemberSignedUpEvent(testMember.getMemberId()));
                return null;
            });

            // then
            // 트랜잭션 커밋 후 saveAll이 1번 호출되었는지 검증
            verify(simpleTimerRepository, times(1)).saveAll(any());
        }
    }

    @Nested
    @DisplayName("비동기 실행 동작")
    class AsyncExecutionTest {
        @BeforeEach
        void setUp() {
            reset(simpleTimerRepository);
        }

        @Test
        @DisplayName("createDefaultSimpleTimer는 비동기로 실행되지만, 테스트에서는 동기로 실행된다")
        void createDefaultSimpleTimer_isExecutedSynchronouslyInTest() {
            // given
            Member testMember = createAndSaveTestMember();
            setupDefaultPolicyMock();
            final String mainThreadName = Thread.currentThread().getName();
            final String[] asyncThreadName = {null};

            // saveAll 메소드가 호출될 때, 현재 실행중인 스레드의 이름을 기록하도록 설정
            doAnswer(invocation -> {
                asyncThreadName[0] = Thread.currentThread().getName();
                return null;
            }).when(simpleTimerRepository).saveAll(any());

            // when
            simpleTimerCommandService.createDefaultSimpleTimer(new MemberSignedUpEvent(testMember.getMemberId()));

            // then
            // AsyncTestConfig로 인해 서비스 메소드가 동기적으로 실행됨. 따라서 테스트를 실행한 메인 쓰레드 이름과 saveAll이 실행된 스레드의 이름이 같은지 비교
            Assertions.assertThat(asyncThreadName[0]).isEqualTo(mainThreadName);
        }

        @Test
        @DisplayName("타이머 저장 실패 시 예외가 전파되지 않고 로그가 기록된다")
        void createDefaultSimpleTimer_handlesRepositoryException() {
            // given
            Member testMember = createAndSaveTestMember();
            setupDefaultPolicyMock();

            doThrow(new RuntimeException("DB 저장 실패")).when(simpleTimerRepository).saveAll(any());

            // when & then
            // 예외가 전파되지 않음을 확인
            assertDoesNotThrow(() -> {
                simpleTimerCommandService.createDefaultSimpleTimer(new MemberSignedUpEvent(testMember.getMemberId()));
            });
        }
    }
}