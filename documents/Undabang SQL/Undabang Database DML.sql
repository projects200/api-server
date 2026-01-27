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

INSERT INTO policy_groups (policy_groups_id, policy_groups_name)
VALUES (5, 'exercise-location');

INSERT INTO policies (policy_id, policy_key, policy_value, policy_unit, policy_description)
values (20, 'EXERCISE_LOCATION_MAX_COUNT', '10',
        'COUNT', '운동 기록이 가질 수 있는 최대 갯수');

INSERT INTO notification_types (notification_type_code, notification_type_category, notification_type_default_enabled,
                                notification_type_is_active, notification_type_created_at)
VALUES ('CHAT_MESSAGE', 'PERSONAL', TRUE, TRUE, NOW());

-- 선택할 수 있는 운동 이미지 생성
INSERT INTO exercise_types (exercise_name, exercise_type_image_url)
VALUES ('레슬링', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/wrestling.png'),
       ('수구', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/water_polo.png'),
       ('배구', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/volleyball.png'),
       ('테니스', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/tennis.png'),
       ('태권도', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/taekwondo.png'),
       ('탁구',
        'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/table_tennis.png'),
       ('스쿼시', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/squash.png'),
       ('축구', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/soccer.png'),
       ('스키', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/ski.png'),
       ('런닝', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/running.png'),
       ('럭비', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/rugby.png'),
       ('조정', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/rowing.png'),
       ('파워 리프팅',
        'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/power_lifting.png'),
       ('필라테스', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/pilates.png'),
       ('마라톤', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/marathon.png'),
       ('핸드볼', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/handball.png'),
       ('골프', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/golf.png'),
       ('게이트볼', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/gate_ball.png'),
       ('풋살', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/futsal.png'),
       ('헬스', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/fitness.png'),
       ('낚시', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/fishhook.png'),
       ('펜싱', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/fencing.png'),
       ('컬링', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/curling.png'),
       ('크로스핏', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/crossfit.png'),
       ('크리켓', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/cricket.png'),
       ('복싱', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/boxing.png'),
       ('볼링', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/bowling.png'),
       ('자전거', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/bicycle.png'),
       ('농구', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/basketball.png'),
       ('야구', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/baseball.png'),
       ('배드민턴', 'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/badminton.png'),
       ('미식축구',
        'https://undabang-public-assets.s3.ap-northeast-2.amazonaws.com/images/exercises/American_football.png');

INSERT INTO policies (policy_key, policy_value, policy_unit, policy_description)
VALUES (21, 'PREFERRED_EXERCISE_MAX_COUNT', '5', 'COUNT', '선호 운동 최대 보유 갯수');

INSERT INTO policies (policy_id, policy_key, policy_value, policy_unit, policy_description)
VALUES (22, 'EXERCISE_LOCATION_MAX_DISTANCE_METER', '5000', 'METERS', '현재 사용자와 운동장소간의 허용 가능한 최대 거리');

INSERT INTO policy_group_mappings (policy_id, policy_groups_id)
VALUES (22, 5);

INSERT INTO feed_types (feed_type_name, feed_type_desc, feed_type_is_active)
VALUES ('헬스 다방', '헬스와 웨이트 트레이닝을 즐기는 사람들이 모이는 피드입니다.', 1),
       ('런닝 다방', '러닝과 조깅 기록을 공유하는 피드입니다.', 1),
       ('축구 다방', '축구를 사랑하는 사람들이 소통하는 피드입니다.', 1),
       ('농구 다방', '농구 경기와 플레이 이야기를 나누는 피드입니다.', 1),
       ('자전거 다방', '라이딩 코스와 자전거 운동을 공유하는 피드입니다.', 1),
       ('골프 다방', '골프 라운딩과 연습 기록을 나누는 피드입니다.', 1),
       ('배드민턴 다방', '배드민턴을 즐기는 사람들의 피드입니다.', 1),
       ('테니스 다방', '테니스 플레이와 레슨 정보를 공유하는 피드입니다.', 1),
       ('필라테스 다방', '필라테스 운동과 자세를 기록하는 피드입니다.', 1),
       ('요가 다방', '요가와 스트레칭을 통해 몸과 마음을 가꾸는 피드입니다.', 1),
       ('풋살 다방', '풋살 경기와 팀 활동을 공유하는 피드입니다.', 1),
       ('마라톤 다방', '마라톤 도전과 대회 준비 과정을 나누는 피드입니다.', 1),
       ('볼링 다방', '볼링 점수와 플레이 경험을 공유하는 피드입니다.', 1),
       ('복싱 다방', '복싱 훈련과 스파링 이야기를 나누는 피드입니다.', 1),
       ('크로스핏 다방', '고강도 크로스핏 운동을 기록하는 피드입니다.', 1),
       ('탁구 다방', '탁구 경기와 연습 영상을 공유하는 피드입니다.', 1),
       ('야구 다방', '야구 경기와 사회인 야구 활동을 나누는 피드입니다.', 1),
       ('배구 다방', '배구를 즐기는 사람들의 소통 공간입니다.', 1),
       ('태권도 다방', '태권도 수련과 승급 이야기를 공유하는 피드입니다.', 1),
       ('스키 다방', '스키 시즌과 슬로프 후기를 나누는 피드입니다.', 1),
       ('파워 리프팅 다방', '파워 리프팅 기록과 훈련을 공유하는 피드입니다.', 1),
       ('펜싱 다방', '펜싱 훈련과 경기 경험을 나누는 피드입니다.', 1),
       ('핸드볼 다방', '핸드볼 경기와 팀 활동을 공유하는 피드입니다.', 1),
       ('럭비 다방', '럭비를 즐기는 사람들의 피드입니다.', 1),
       ('레슬링 다방', '레슬링 훈련과 기술을 공유하는 피드입니다.', 1),
       ('조정 다방', '조정 운동과 수상 훈련을 기록하는 피드입니다.', 1),
       ('수구 다방', '수구 경기와 훈련을 공유하는 피드입니다.', 1),
       ('컬링 다방', '컬링 경기와 전략을 나누는 피드입니다.', 1),
       ('크리켓 다방', '크리켓 경기를 즐기는 사람들의 피드입니다.', 1),
       ('게이트볼 다방', '게이트볼을 즐기는 커뮤니티 피드입니다.', 1),
       ('낚시 다방', '낚시 기록과 조과를 공유하는 피드입니다.', 1),
       ('미식축구 다방', '미식축구 경기와 전술 이야기를 나누는 피드입니다.', 1);
