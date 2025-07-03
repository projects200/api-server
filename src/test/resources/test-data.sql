INSERT INTO policies (policy_key, policy_value, policy_unit, policy_description)
VALUES
    -- 점수 범위 정책
    ('EXERCISE_SCORE_MAX_POINTS', '100', 'POINTS', '회원이 가질 수 있는 최대 운동 점수'),
    ('EXERCISE_SCORE_MIN_POINTS', '0', 'POINTS', '회원이 가질 수 있는 최소 운동 점수'),

    -- 점수 획득 정책
    ('SIGNUP_INITIAL_POINTS', '35', 'POINTS', '회원 가입 시 기본으로 부여되는 점수'),
    ('POINTS_PER_EXERCISE', '3', 'POINTS', '운동 기록 1회당 부여되는 점수 (일 1회)'),
    ('EXERCISE_RECORD_VALIDITY_PERIOD', '2', 'DAYS', '점수 획득이 가능한 운동 기록의 유효 기간. (값: 2, 단위: DAYS)'),

    -- 점수 차감 (페널티) 정책
    ('PENALTY_INACTIVITY_THRESHOLD_DAYS', '7', 'DAYS', '페널티가 시작되는 비활성 기준일 (이 기간 이상 운동 기록이 없을 경우)'),
    ('PENALTY_SCORE_DECREMENT_POINTS', '1', 'POINTS', '비활성 상태일 때 매일 차감되는 점수');