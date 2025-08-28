INSERT INTO post_type (post_type_name, post_type_desc)
VALUES ('오운완 게시판', '오늘의 운동한 모습이나 결과를 자랑하는 게시판입니다');

INSERT INTO comment_report_subjects (comment_report_subject_name)
VALUES ('스팸홍보/도배입니다.'),
       ('음란물입니다.'),
       ('불법정보를 포함하고 있습니다.'),
       ('청소년에게 유해한 내용입니다.'),
       ('욕설/생명경시/혐오/차별적 표현입니다.'),
       ('개인정보가 노출되었습니다.'),
       ('불쾌한 표현이 있습니다.'),
       ('기타');

INSERT INTO member_report_subjects (member_report_subject_name)
VALUES ('사용자 사진에 음란물이 있습니다.'),
       ('사용자 정보에 불법정보를 포함하고 있습니다.'),
       ('사용자 정보에 청소년에게 유해한 내용이 있습니다.'),
       ('사용자 정보에 욕설/생명경시/혐오/차별적 표현이 있습니다.'),
       ('사용자 정보에 개인정보가 노출되었습니다.'),
       ('사용자 정보에 불쾌한 표현이 있습니다.'),
       ('약속된 운동에 상습적으로 무단 불참하였습니다.'),
       ('기타');

INSERT INTO post_report_subjects (post_report_subject_name)
VALUES ('스팸홍보/도배입니다.'),
       ('음란물입니다.'),
       ('불법정보를 포함하고 있습니다.'),
       ('청소년에게 유해한 내용입니다.'),
       ('욕설/생명경시/혐오/차별적 표현입니다.'),
       ('개인정보가 노출되었습니다.'),
       ('불쾌한 표현이 있습니다.'),
       ('기타');

INSERT INTO policies (policy_id, policy_key, policy_value, policy_unit, policy_description)
VALUES
    -- 점수 범위 정책
    (1, 'EXERCISE_SCORE_MAX_POINTS', '100', 'POINTS', '회원이 가질 수 있는 최대 운동 점수'),
    (2, 'EXERCISE_SCORE_MIN_POINTS', '0', 'POINTS', '회원이 가질 수 있는 최소 운동 점수'),

    -- 점수 획득 정책
    (3, 'SIGNUP_INITIAL_POINTS', '35', 'POINTS', '회원 가입 시 기본으로 부여되는 점수'),
    (4, 'POINTS_PER_EXERCISE', '3', 'POINTS', '운동 기록 1회당 부여되는 점수 (일 1회)'),
    (5, 'EXERCISE_RECORD_VALIDITY_PERIOD', '2', 'DAYS', '점수 획득이 가능한 운동 기록의 유효 기간. (단위: DAYS, HOURS, MINUTES)'),
    (6, 'EXERCISE_RECORD_MAX_PER_DAY', '1', 'COUNT', '하루에 기록할 수 있는 최대 운동 횟수'),

    -- 점수 차감 (페널티) 정책
    (7, 'PENALTY_INACTIVITY_THRESHOLD_DAYS', '7', 'DAYS', '페널티가 시작되는 비활성 기준일 (이 기간 이상 운동 기록이 없을 경우)'),
    (8, 'PENALTY_SCORE_DECREMENT_POINTS', '1', 'POINTS', '비활성 상태일 때 매일 차감되는 점수');

INSERT INTO policy_groups (policy_groups_id, policy_groups_name)
VALUES (1, 'exercise-score');

INSERT INTO policy_group_mappings (policy_id, policy_groups_id)
VALUES (1, 1),
       (2, 1),
       (3, 1),
       (4, 1),
       (5, 1),
       (6, 1),
       (7, 1),
       (8, 1);

-- 시퀀스 초기값 설정
insert into BATCH_JOB_EXECUTION_SEQ (ID, UNIQUE_KEY)
values (0, '0');
insert into BATCH_JOB_SEQ (ID, UNIQUE_KEY)
values (0, '0');
insert into BATCH_STEP_EXECUTION_SEQ (ID, UNIQUE_KEY)
values (0, '0');


-- 정책 테이블에 푸시 알림 관련 데이터 추가
INSERT INTO policies (policy_id, policy_key, policy_value, policy_unit, policy_description)
VALUES
    -- 푸시 알림 전체 활성화 여부 (마스터 스위치)
    (9, 'NOTIFICATION_ENABLED', 'true', 'BOOLEAN', '전체 푸시 알림 기능 활성화 여부 (true/false)'),

    -- 점수 차감 전 알림 정책
    (10, 'NOTIFICATION_PRE_INACTIVITY_START_DAYS', '2', 'DAYS', '점수 차감 D-day 며칠 전부터 알림을 보낼지'),
    (11, 'NOTIFICATION_PRE_INACTIVITY_INTERVAL_DAYS', '1', 'DAYS', '점수 차감 전 알림을 며칠 간격으로 보낼지 (매일=1)'),

    -- 점수 차감 후 알림 정책
    (12, 'NOTIFICATION_POST_INACTIVITY_INTERVAL_DAYS', '7', 'DAYS', '점수 차감 시작 후 알림을 며칠 간격으로 보낼지 (일주일=7)'),

    -- 알림 중단 정책
    (13, 'NOTIFICATION_SCORE_THRESHOLD_MIN', '0', 'POINTS', '회원 점수가 이 값 이하일 경우 더 이상 알림을 보내지 않음'),

    -- 알림 보내는 시간
    (14, 'NOTIFICATION_SEND_TIME', '18', 'HOURS', '알림을 보내는 시간 (24시간 형식, 예: 18시 = 18)');

INSERT INTO policy_groups(policy_groups_id, policy_groups_name)
VALUES (2, 'notification');

INSERT INTO policy_group_mappings (policy_id, policy_groups_id)
VALUES (9, 2),  -- NOTIFICATION_ENABLED
       (10, 2), -- NOTIFICATION_PRE_INACTIVITY_START_DAYS
       (11, 2), -- NOTIFICATION_PRE_INACTIVITY_INTERVAL_DAYS
       (12, 2), -- NOTIFICATION_POST_INACTIVITY_INTERVAL_DAYS
       (13, 2), -- NOTIFICATION_SCORE_THRESHOLD_MIN
       (14, 2);
-- NOTIFICATION_SEND_TIME

-- 3-1. 알림 시나리오 정의
INSERT INTO notification_scenarios (scenario_code, scenario_description, scenario_is_enabled)
VALUES ('PRE_INACTIVITY_REMINDER', '점수 차감 전 사용자에게 보내는 리마인드 알림', TRUE),
       ('POST_INACTIVITY_NUDGE', '점수 차감 시작 후 사용자에게 보내는 복귀 유도 알림', TRUE);

-- 3-2. 알림 메시지 내용 정의
INSERT INTO notification_messages (message_id, message_title, message_body)
VALUES (1, NULL, '잠깐! 소중한 운동 점수가 변동될 수 있어요. 가볍게라도 운동하고 지금의 점수를 지켜볼까요?'),
       (2, NULL, '혹시… 저희 앱 삭제하신 줄 알았어요! 돌아오셔서 반가워요. 운동하러 가볼까요?'),
       (3, NULL, '주문하신 커피가 식고 있어요… 운다방에 돌아와 주세요 🥺'),
       (4, NULL, '점수 회복 챌린지 시작! 지난주보다 더 나은 점수를 위해, 오늘부터 다시 꾸준히 운동해 볼까요? 💪');

-- 3-3. 시나리오와 메시지 매핑
-- PRE_INACTIVITY_REMINDER 시나리오 (ID: 1)에는 메시지 (ID: 1)를 연결
INSERT INTO scenario_message_mappings (scenario_id, message_id)
VALUES (1, 1);

-- POST_INACTIVITY_NUDGE 시나리오 (ID: 2)에는 메시지 (ID: 2, 3, 4)를 연결
INSERT INTO scenario_message_mappings (scenario_id, message_id)
VALUES (2, 2),
       (2, 3),
       (2, 4);

-- 'simple-timer' 정책 그룹을 생성
INSERT INTO policy_groups (policy_groups_id, policy_groups_name)
VALUES (3, 'simple-timer');

-- SIMPLE_TIMER_INIT_COUNT 정책을 생성
INSERT INTO policies (policy_id, policy_key, policy_value, policy_unit, policy_description)
VALUES (15, 'SIMPLE_TIMER_INIT_COUNT', '6',
        'COUNT', '심플 타이머의 초기 설정 갯수');

INSERT INTO policy_group_mappings (policy_id, policy_groups_id)
VALUES (15, 3);

-- SIMPLE_TIMER_INIT_VALUES 정책을 생성
INSERT INTO policies (policy_id, policy_key, policy_value, policy_unit, policy_description)
VALUES (16, 'SIMPLE_TIMER_INIT_VALUES', '30,40,50,60,75,90',
        'SECONDS', '심플 타이머의 초기 설정 값. 회원 가입시 추가해서 사용해야 함');

INSERT INTO policy_group_mappings (policy_id, policy_groups_id)
VALUES (16, 3);

-- SIMPLE_TIMER_MAX_COUNT 정책을 생성
INSERT INTO policies (policy_id, policy_key, policy_value, policy_unit, policy_description)
VALUES (17, 'SIMPLE_TIMER_MAX_COUNT', '6',
        'COUNT', '심플 타이머가 가질 수 있는 최대 갯수');

INSERT INTO policy_group_mappings (policy_id, policy_groups_id)
VALUES (17, 3);

-- 'custom-timer' 정책 그룹을 생성
INSERT INTO policy_groups (policy_groups_id, policy_groups_name)
VALUES (4, 'custom-timer');


-- CUSTOM_TIMER_STEP_MAX_COUNT 정책을 생성
INSERT INTO policies (policy_id, policy_key, policy_value, policy_unit, policy_description)
VALUES (18, 'CUSTOM_TIMER_STEP_MAX_COUNT', '50',
        'COUNT', '커스텀 타이머 스텝이 가질 수 있는 최대 갯수');

INSERT INTO policy_group_mappings (policy_id, policy_groups_id)
VALUES (18, 4);

-- CUSTOM_TIMER_STEP_MIN_COUNT 정책을 생성
INSERT INTO policies (policy_id, policy_key, policy_value, policy_unit, policy_description)
VALUES (19, 'CUSTOM_TIMER_STEP_MIN_COUNT', '1',
        'COUNT', '커스텀 타이머 스텝이 가질 수 있는 최소 갯수');

INSERT INTO policy_group_mappings (policy_id, policy_groups_id)
VALUES (19, 4);