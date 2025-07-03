package com.project200.undabang.common.config;

import com.project200.undabang.policy.provider.PolicyProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class CacheWarmerTest {
    @InjectMocks // 테스트 대상
    private CacheWarmer cacheWarmer;

    @Mock // 의존성 Mocking
    private PolicyProvider policyProvider;

    @Mock // ApplicationRunner.run()의 파라미터 Mocking
    private ApplicationArguments applicationArguments;

    @Test
    @DisplayName("성공 케이스: CacheWarmer가 정상적으로 PolicyProvider를 호출한다")
    void run_Success() throws Exception {
        // given: 아무런 설정도 하지 않으면, policyProvider는 정상적으로 동작하는 것처럼 보임

        // when: CacheWarmer의 run 메소드를 직접 실행
        cacheWarmer.run(applicationArguments);

        // then: policyProvider.getAllPoliciesAsMap()이 정확히 1번 호출되었는지 검증
        then(policyProvider).should(times(1)).getAllPoliciesAsMap(); // 캐시 초기화도 확인
    }

    @Test
    @DisplayName("실패 케이스: PolicyProvider에서 예외 발생 시, RuntimeException을 다시 던진다")
    void run_Fail_WhenProviderThrowsException() {
        // given: PolicyProvider가 예외를 던지도록 설정
        given(policyProvider.getAllPoliciesAsMap())
                .willThrow(new IllegalStateException("의도된 DB 연결 실패"));

        // when & then: cacheWarmer.run()을 실행했을 때, 우리가 정의한 RuntimeException이 발생하는지 검증
        assertThatThrownBy(() -> cacheWarmer.run(applicationArguments))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("캐시 예열 실패!")
                .cause() // 근본 원인 예외까지 검증
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("의도된 DB 연결 실패");
    }
}
