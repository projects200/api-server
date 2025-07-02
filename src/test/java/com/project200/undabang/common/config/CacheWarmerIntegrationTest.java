package com.project200.undabang.common.config;

import com.project200.undabang.policy.entity.Policy;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.provider.PolicyProvider;
import com.project200.undabang.policy.provider.impl.PolicyProviderImpl;
import com.project200.undabang.policy.repository.PolicyRepository;
import com.project200.undabang.policy.service.impl.PolicyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@SpringBootTest(classes = {
        CacheWarmer.class,
        PolicyProviderImpl.class,
        PolicyServiceImpl.class,
        PolicyRepository.class
//        CacheWarmerIntegrationTest.TestConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CacheWarmerIntegrationTest {

    @Autowired
    private PolicyProvider policyProvider;

    @Autowired
    private CacheManager cacheManager;

    @MockitoSpyBean
    private PolicyRepository policyRepository;

//    @Configuration
//    @EnableAutoConfiguration // <-- 데이터베이스, JPA 등 필수적인 자동 설정을 켜줍니다.
//    @EnableJpaRepositories(basePackages = "com.project200.undabang.policy.repository") // <-- 리포지토리 위치
//    @EntityScan(basePackages = "com.project200.undabang.policy.entity") // <-- 엔티티 위치
//    static class TestConfig {
//        // 이 클래스는 설정을 위한 용도이므로 내부는 비워둡니다.
//    }

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
        // DB 조회(findAll)가 CacheWarmer에 의해 정확히 1번만 호출되었는지 검증
        then(policyRepository).should().findAll();

        // 'policies' 캐시 생성 확인
        Cache policiesCache = cacheManager.getCache("policies");
        assertThat(policiesCache).isNotNull();

        // 캐시 안에 실제로 데이터가 들어있는지 확인
        // getAllPoliciesAsMap()이 Map<PolicyKey, Policy>를 반환하므로, 해당 타입으로 캐시 값 조회
        @SuppressWarnings("unchecked")
        Map<PolicyKey, Policy> cachedMap = policiesCache.get("policies", Map.class);
        assertThat(cachedMap).isNotNull();
        assertThat(cachedMap).hasSize(2);
        assertThat(cachedMap.get(PolicyKey.EXERCISE_SCORE_MAX_POINTS)).isNotNull();
        assertThat(cachedMap.get(PolicyKey.EXERCISE_SCORE_MIN_POINTS)).isNotNull();
    }
}