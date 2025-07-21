package com.project200.undabang.common.config;

import com.project200.undabang.policy.provider.PolicyProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시점에 주요 데이터를 캐시에 미리 로드(예열)하는 역할을 수행합니다.
 * ApplicationRunner를 구현하여 Spring Boot 애플리케이션이 완전히 시작된 후 실행됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class CacheWarmer implements ApplicationRunner {
    private final PolicyProvider policyProvider;
    /**
     * Spring Boot 애플리케이션이 시작될 때 단 한 번 호출되는 메소드입니다.
     * 이 메소드에서 정책 데이터를 미리 조회하여 캐시에 저장합니다.
     *
     * @param args 애플리케이션 실행 시 전달된 인자
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("==================== 캐시 예열 시작 ====================");

        try {
            log.info("[CacheWarming] 정책(Policies) 데이터 캐싱을 시작합니다...");
            policyProvider.getAllPoliciesAsMap();
            log.info("[CacheWarming] >>> 정책(Policies) 데이터 캐싱 성공!");

            // --- 다른 캐시 예열 작업이 필요하다면 여기에 추가 ---

        } catch (Exception e) {
            // 예외 발생 시 로깅 및 처리
            log.error("[CacheWarming] 캐시 예열 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("캐시 예열 실패!", e);
        }

        log.info("==================== 캐시 예열 완료 ====================");
    }
}
