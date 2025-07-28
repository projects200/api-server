INSERT INTO policies (policy_key, policy_value, policy_unit, policy_description, policy_updated_at, policy_created_at)
VALUES
    -- 점수 범위 정책
    ('EXERCISE_SCORE_MAX_POINTS', '100', 'POINTS', '회원이 가질 수 있는 최대 운동 점수', '2021-01-01 00:00:00', '2021-01-01 00:00:00'),
    ('EXERCISE_SCORE_MIN_POINTS', '0', 'POINTS', '회원이 가질 수 있는 최소 운동 점수', '2021-01-01 00:00:00', '2021-01-01 00:00:00'),

    -- 점수 획득 정책
    ('SIGNUP_INITIAL_POINTS', '35', 'POINTS', '회원 가입 시 기본으로 부여되는 점수', '2021-01-01 00:00:00', '2021-01-01 00:00:00'),
    ('POINTS_PER_EXERCISE', '3', 'POINTS', '운동 기록 1회당 부여되는 점수 (일 1회)', '2021-01-01 00:00:00', '2021-01-01 00:00:00'),
    ('EXERCISE_RECORD_VALIDITY_PERIOD', '2', 'DAYS', '점수 획득이 가능한 운동 기록의 유효 기간. (단위: DAYS, HOURS, MINUTES)', '2021-01-01 00:00:00', '2021-01-01 00:00:00'),
    ('EXERCISE_RECORD_MAX_PER_DAY', '1', 'COUNT', '하루에 기록할 수 있는 최대 운동 횟수', '2021-01-01 00:00:00', '2021-01-01 00:00:00'),

    -- 점수 차감 (페널티) 정책
    ('PENALTY_INACTIVITY_THRESHOLD_DAYS', '7', 'DAYS', '페널티가 시작되는 비활성 기준일 (이 기간 이상 운동 기록이 없을 경우)', '2021-01-01 00:00:00', '2021-01-01 00:00:00'),
    ('PENALTY_SCORE_DECREMENT_POINTS', '1', 'POINTS', '비활성 상태일 때 매일 차감되는 점수', '2021-01-01 00:00:00', '2021-01-01 00:00:00');

-- test-data.sql

-- PolicyGroup 추가
INSERT INTO policy_groups (policy_groups_name, policy_groups_created_at, policy_groups_updated_at) VALUES
    ('exercise-score', '2021-01-01 00:00:00', '2021-01-01 00:00:00');

-- PolicyGroupMapping 추가 (policy_id는 test-data.sql에 이미 정의된 값을 사용해야 합니다)
INSERT INTO policy_group_mappings (mapping_id, policy_id, policy_groups_id) VALUES
    (1, 1, 1), -- EXERCISE_SCORE_MAX_POINTS
    (2, 2, 1), -- EXERCISE_SCORE_MIN_POINTS
    (3, 3, 1), -- SIGNUP_INITIAL_POINTS
    (4, 4, 1), -- POINTS_PER_EXERCISE
    (5, 5, 1), -- EXERCISE_RECORD_VALIDITY_PERIOD
    (6, 6, 1), -- EXERCISE_RECORD_MAX_PER_DAY
    (7, 7, 1), -- PENALTY_INACTIVITY_THRESHOLD_DAYS
    (8, 8, 1); -- PENALTY_SCORE_DECREMENT_POINTS
