package com.project200.undabang.common.config;

import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.provider.PolicyProvider;
import com.project200.undabang.policy.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CacheWarmerIntegrationTest {

    @Autowired
    private PolicyProvider policyProvider;

    @Autowired
    private CacheManager cacheManager;

    @MockitoSpyBean
    private PolicyRepository policyRepository;

    @TestConfiguration
    @EnableCaching
    static class CacheTestConfig {
        // 내부는 비워둡니다. @EnableCaching 어노테이션 자체가 목적입니다.
    }

    @BeforeEach
    void setUp() {
        List<Policy> mockPolicies = List.of(
                Policy.builder()
                        .policyKey(PolicyKey.EXERCISE_SCORE_MAX_POINTS)
                        .policyValue("100")
                        .policyUnit("POINTS")
                        .policyDescription("회원이 가질 수 있는 최대 운동 점수")
                        .build(),
                Policy.builder()
                        .policyKey(PolicyKey.EXERCISE_SCORE_MIN_POINTS)
                        .policyValue("0")
                        .policyUnit("POINTS")
                        .policyDescription("회원이 가질 수 있는 최소 운동 점수")
                        .build()

        );

        // SpyBean의 실제 메소드 호출 대신 Mock 결과를 반환하도록 설정
        given(policyRepository.findAll()).willReturn(mockPolicies);
    }

    @Test
    @DisplayName("애플리케이션 시작 시 CacheWarmer가 실행되어 정책 캐시가 예열된다")
    void cacheShouldBeWarmedUpOnStartup() {
        // then
        // 애플리케이션 시작 시 CacheWarmer에 의해 캐시가 이미 예열된 상태
        // 배치 등에서 캐시 적용 전에 호출하므로 인해 호출 횟수가 1로 보장되지 않아 최소 1회로 지정
        then(policyRepository).should(atLeast(1)).findAll();

        // 'policies' 캐시 생성 확인
        Cache policiesCache = cacheManager.getCache("policies");
        assertThat(policiesCache).isNotNull();

        // 파라미터 없는 메소드는 SimpleKey.EMPTY를 키로 사용하므로, 올바른 키로 데이터를 조회합니다.
        Cache.ValueWrapper cachedValueWrapper = policiesCache.get(SimpleKey.EMPTY);
        assertThat(cachedValueWrapper).isNotNull();

        // 캐시 안에 실제로 데이터가 들어있는지 확인
        @SuppressWarnings("unchecked")
        Map<PolicyKey, Policy> cachedMap = (Map<PolicyKey, Policy>) cachedValueWrapper.get();
        assertThat(cachedMap).isNotNull();
        assertThat(cachedMap.get(PolicyKey.EXERCISE_SCORE_MAX_POINTS)).isNotNull();
        assertThat(cachedMap.get(PolicyKey.EXERCISE_SCORE_MIN_POINTS)).isNotNull();
    }

    @Test
    @DisplayName("캐시 예열 후, 정책을 조회하면 DB를 다시 호출하지 않고 캐시를 사용한다")
    void shouldUseCacheAfterWarmingUp() {
        // given
        // 애플리케이션 시작 시 CacheWarmer에 의해 캐시가 이미 예열된 상태
        // 배치 등에서 캐시 적용 전에 호출하므로 인해 호출 횟수가 1로 보장되지 않아 최소 1회로 지정
        then(policyRepository).should(atLeast(1)).findAll();

        // when
        // 정책 조회 메소드를 다시 한번 호출
        policyProvider.getAllPoliciesAsMap();

        // then
        // 총 호출 횟수: 예열 시 1번
        then(policyRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("캐시를 무효화(refresh)하면, 다음 조회 시 DB를 다시 호출하여 캐시를 갱신한다")
    void shouldReloadCacheAfterEviction() {
        // given
        // 애플리케이션 시작 시 CacheWarmer에 의해 캐시가 이미 예열된 상태
        // 배치 등에서 캐시 적용 전에 호출하므로 인해 호출 횟수가 1로 보장되지 않아서 현재까지 호출 횟수를 확인합니다.
        int initialInvocationCount = Math.toIntExact(BDDMockito.mockingDetails(policyRepository).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("findAll"))
                .count());
        then(policyRepository).should(times(initialInvocationCount)).findAll();

        // when
        // 캐시 무효화
        policyProvider.refreshPolicies();


        // 정책 재조회
        policyProvider.getAllPoliciesAsMap();

        // then
        // 총 호출 횟수: 기존 예열 호출 횟수 + 1
        then(policyRepository).should(times(initialInvocationCount + 1)).findAll();
    }
}