package com.project200.undabang.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thumbnail 관련 설정 값을 application.yml에서 바인딩하는 record 클래스입니다.
 *
 * @param width  썸네일의 기본 너비
 * @param height 썸네일의 기본 높이
 */
@ConfigurationProperties("thumbnail.profile")
public record ThumbnailProperties(int width, int height) {
}
