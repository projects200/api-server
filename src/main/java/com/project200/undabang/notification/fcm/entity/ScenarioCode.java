package com.project200.undabang.notification.fcm.entity;

public enum ScenarioCode {
    PRE_INACTIVITY_REMINDER,    // 점수 차감 전 사용자에게 보내는 리마인드 알림
    POST_INACTIVITY_NUDGE      // 점수 차감 시작 후 사용자에게 보내는 복귀 유도 알림
}
