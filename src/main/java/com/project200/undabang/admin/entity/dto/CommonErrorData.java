package com.project200.undabang.admin.entity.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public final class CommonErrorData {
    private final String serviceName; // 에러가 나는 서비스 이름
    private final ErrorLevel errorLevel; // 에러의 심각도
    private final LocalDateTime errorOccurredAt; // 에러 발생 시간
    private final String summary; // 에러 내용을 빠르게 볼 수 있는 요약 메세지
    private final String exceptionName; // 에러가 발생한 클래스 이름
    private final String stackTrace; // 예외 발생 지점
    private final String environment; // 오류 발생 환경
    private final String actionGuide; // 개발자가 취해야할 조치 가이드 및 관련 문서 링크
}
