package com.project200.undabang.admin.entity.dto;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
public abstract class CommonErrorDto {
    protected String serviceName; // 에러가 나는 서비스 이름
    protected String className; // 에러가 발생한 클래스 이름
    protected ErrorLevel errorLevel; // 에러의 심각도
    protected String summary; // 에러 내용을 빠르게 볼 수 있는 요약 메세지
    protected LocalDateTime errorOccurredAt; // 에러 발생 시간
    protected String stackTrace; // 예외 발생 지점
    protected String environment; // 오류 발생 환경
    protected String actionGuide; // 개발자가 취해야할 조치 가이드 및 관련 문서 링크

    public abstract String formattingMessage();
}
