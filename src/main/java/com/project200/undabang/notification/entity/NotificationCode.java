package com.project200.undabang.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시스템의 핵심 비즈니스 로직과 강하게 결합되어 타입-안전하게 참조되어야 하는
 * 알림 타입 코드를 정의한 Enum입니다.
 *
 * <p><b>매우 중요:</b> 이 Enum은 시스템에 존재하는 <u>모든 알림 타입을 대표하지 않습니다.</u>
 * 모든 알림 타입의 마스터 데이터(Source of Truth)는 'notification_types' DB 테이블입니다.
 * 이 Enum은 DB에 있는 특정 코드 값에 대한 타입-안전한 별명(Alias) 역할을 합니다.</p>
 *
 * <p>관리 도구를 통해 DB에 새로운 알림 타입이 추가되더라도,
 * 핵심 비즈니스 로직과 직접적인 관련이 없다면 이 Enum에 추가할 필요가 없습니다.</p>
 */

@Getter
@RequiredArgsConstructor
public enum NotificationCode {
    CHAT_MESSAGE("CHAT_MESSAGE"),
    WORKOUT_REMINDER("WORKOUT_REMINDER");

    private final String code; // DB의 notification_type_code 컬럼과 매칭되는 값
}
