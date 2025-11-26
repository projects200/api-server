package com.project200.undabang.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * NotificationCategory는 알림 유형의 상위 분류를 정의하는 Enum입니다.
 * 알림 데이터를 분류하고 관리하기 위해 사용됩니다.
 * <p>
 * PERSONAL: 개인 알림에 해당하는 카테고리입니다.
 * NOTICE: 공지 또는 시스템 전체 알림에 해당하는 카테고리입니다.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationCategory {
    PERSONAL,
    NOTICE
}