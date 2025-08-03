package com.project200.undabang.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Firebase 관련 설정 값을 application.yml에서 바인딩하는 record 클래스입니다.
 *
 * @param enabled Firebase 기능 활성화 여부
 * @param credentials Firebase 인증 정보
 * @param credentials.path Firebase 인증 파일 경로
 */
@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(
        boolean enabled,
        Credentials credentials
) {
    public record Credentials(String path) {
    }
}