package com.project200.undabang.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 설정 적용
                .allowedOrigins(allowedOrigins) // 프로퍼티 파일에서 읽어온 허용 출처 설정
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // 허용할 HTTP 메소드
                .allowedHeaders("*") // 허용할 요청 헤더
                .allowCredentials(true) // 쿠키를 포함한 요청 허용 여부
                .maxAge(3600); // pre-flight 요청의 캐시 시간 (초)
    }
}
