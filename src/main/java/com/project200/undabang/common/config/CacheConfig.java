package com.project200.undabang.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    /**
     * 프로젝트에서 사용할 기본 캐시 매니저를 생성하여 스프링 컨테이너에 빈으로 등록합니다.
     * <p>
     * 이 캐시 매니저는 {@link ConcurrentMapCacheManager}를 사용하여 별도의 외부 라이브러리 없이
     * JDK의 {@link java.util.concurrent.ConcurrentHashMap}을 기반으로 동작하는 인메모리 캐시를 제공합니다.
     * <p>
     * 애플리케이션 내에서 사용되는 모든 명명된 캐시("policyGroups", "policies")를 파라미터로 전달하여,
     * 하나의 캐시 매니저가 여러 종류의 캐시를 모두 관리할 수 있도록 설정합니다.
     *
     * <ul>
     *     <li><b>policyGroups</b>: 정책 그룹 관련 데이터를 캐싱하는 데 사용됩니다. (신규 기능)</li>
     *     <li><b>policies</b>: 개별 정책 데이터를 캐싱하는 데 사용됩니다. (기존 기능)</li>
     * </ul>
     *
     * @return 프로젝트의 모든 캐시를 관리하는 통합 {@link CacheManager} 인스턴스
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("policyGroups", "policies");
    }
}
