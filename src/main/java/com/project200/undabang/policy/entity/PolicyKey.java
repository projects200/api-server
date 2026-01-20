package com.project200.undabang.policy.entity;

public enum PolicyKey {

    // 점수 범위 정책
    EXERCISE_SCORE_MAX_POINTS, // 회원이 가질 수 있는 최대 운동 점수
    EXERCISE_SCORE_MIN_POINTS, // 회원이 가질 수 있는 최소 운동 점수

    // 점수 획득 정책
    SIGNUP_INITIAL_POINTS, // 회원 가입 시 기본으로 부여되는 점수
    POINTS_PER_EXERCISE, // 운동 기록 1회당 부여되는 점수 (일 1회)
    EXERCISE_RECORD_VALIDITY_PERIOD, // 점수 획득이 가능한 운동 기록의 유효 기간(DAYS, HOURS, MINUTES)
    EXERCISE_RECORD_MAX_PER_DAY, // 하루에 기록할 수 있는 최대 운동 횟수

    // 점수 차감 (페널티) 정책
    PENALTY_INACTIVITY_THRESHOLD_DAYS, // 페널티가 시작되는 비활성 기준일 (이 기간 이상 운동 기록이 없을 경우)
    PENALTY_SCORE_DECREMENT_POINTS, // 비활성 상태일 때 매일 차감되는 점수

    // 서버 푸시 알림
    NOTIFICATION_ENABLED,                           // 전체 푸시 알림 기능 활성화 여부 (true/false)
    NOTIFICATION_PRE_INACTIVITY_START_DAYS,         // 점수 차감 D-day 며칠 전부터 알림을 보낼지
    NOTIFICATION_PRE_INACTIVITY_INTERVAL_DAYS,      // 점수 차감 전 알림을 며칠 간격으로 보낼지 (매일=1)
    NOTIFICATION_POST_INACTIVITY_INTERVAL_DAYS,     // 점수 차감 시작 후 알림을 며칠 간격으로 보낼지 (일주일=7)
    NOTIFICATION_SCORE_THRESHOLD_MIN,               // 회원 점수가 이 값 이하일 경우 더 이상 알림을 보내지 않음
    NOTIFICATION_SEND_TIME,                         // 알림을 보내는 시간 (24시간 형식, 예: 18시 = 18)

    // 심플 타이머
    SIMPLE_TIMER_INIT_COUNT, // 심플 타이머 초기 생성 갯수
    SIMPLE_TIMER_INIT_VALUES, // 심플 타이머 초기 값. 회원 가입시 추가해줘야 함
    SIMPLE_TIMER_MAX_COUNT, // 심플 타이머 최대 보유 갯수

    // 커스텀 타이머
    CUSTOM_TIMER_STEP_MAX_COUNT, // 커스텀 타이머 스텝 최대 보유 갯수
    CUSTOM_TIMER_STEP_MIN_COUNT, // 커스텀 타이머 스텝 최소 보유 갯수

    // 운동 기록
    EXERCISE_LOCATION_MAX_COUNT, // 운동 기록 최대 보유 갯수

    // 선호 운동
    PREFERRED_EXERCISE_MAX_COUNT, // 선호 운동 최대 보유 갯수

    EXERCISE_LOCATION_MAX_DISTANCE_METER
}
