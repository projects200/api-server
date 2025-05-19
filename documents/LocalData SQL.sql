-- 회원 2명 생성 (UUID 사용)
INSERT INTO members (member_id, member_email, member_gender, member_bday, member_nickname, member_desc)
VALUES
    ('550e8400-e29b-41d4-a716-446655440000', 'user11@example.com', 'M', '1990-01-15', '헬스왕', '운동을 사랑하는 30대 남성입니다.'),
    ('550e8400-e29b-41d4-a716-446655440001', 'user22@example.com', 'F', '1995-06-22', '요가여왕', '요가와 필라테스를 즐기는 20대 여성입니다.');

-- 첫 번째 회원의 운동 기록 5개 생성 (ID: 1-5)
INSERT INTO exercises (member_id, exercise_started_at, exercise_ended_at, exercise_title, exercise_detail, exercise_personal_type, exercise_location)
VALUES
    ('550e8400-e29b-41d4-a716-446655440000', '2023-06-01 08:00:00', '2023-06-01 09:30:00', '아침 웨이트 트레이닝', '등, 어깨 집중 운동. 풀업 5세트, 렛풀다운 4세트 완료', '웨이트 트레이닝', '헬스장 더 짐 강남점'),
    ('550e8400-e29b-41d4-a716-446655440000', '2023-06-03 17:30:00', '2023-06-03 19:00:00', '저녁 런닝', '한강공원 10km 러닝 완주. 페이스 5:30/km', '러닝', '한강시민공원'),
    ('550e8400-e29b-41d4-a716-446655440000', '2023-06-05 08:00:00', '2023-06-05 09:00:00', '하체 운동', '스쿼트 100kg 5세트, 레그프레스 120kg 4세트', '웨이트 트레이닝', '헬스장 더 짐 강남점'),
    ('550e8400-e29b-41d4-a716-446655440000', '2023-06-07 06:30:00', '2023-06-07 07:30:00', '아침 수영', '자유형 1km, 평영 500m 완주', '수영', '올림픽 수영장'),
    ('550e8400-e29b-41d4-a716-446655440000', '2023-06-10 19:00:00', '2023-06-10 20:30:00', '가슴/삼두 운동', '벤치프레스 80kg 5세트, 딥스 3세트, 케이블 푸시다운 4세트', '웨이트 트레이닝', '헬스장 더 짐 강남점'),
    ('550e8400-e29b-41d4-a716-446655440000', '2023-06-15 07:00:00', '2023-06-15 08:00:00', '아침 유산소', '러닝머신 30분, 사이클 30분', '유산소', '헬스장 더 짐 강남점'),
    ('550e8400-e29b-41d4-a716-446655440000', '2023-06-15 19:30:00', '2023-06-15 21:00:00', '저녁 상체 운동', '벤치프레스 70kg 5세트, 어깨프레스 40kg 5세트', '웨이트 트레이닝', '헬스장 더 짐 강남점');

-- 두 번째 회원의 운동 기록 5개 생성 (ID: 6-10)
INSERT INTO exercises (member_id, exercise_started_at, exercise_ended_at, exercise_title, exercise_detail, exercise_personal_type, exercise_location)
VALUES
    ('550e8400-e29b-41d4-a716-446655440001', '2023-06-02 10:00:00', '2023-06-02 11:30:00', '모닝 요가', '아쉬탕가 요가 60분 진행, 명상 30분', '요가', '요가스튜디오 서울'),
    ('550e8400-e29b-41d4-a716-446655440001', '2023-06-04 18:00:00', '2023-06-04 19:00:00', '저녁 필라테스', '코어 강화 운동 위주로 진행', '필라테스', '필라테스 센터'),
    ('550e8400-e29b-41d4-a716-446655440001', '2023-06-06 09:00:00', '2023-06-06 10:00:00', '서킷 트레이닝', '8가지 동작 3세트 진행', '서킷 트레이닝', '홈트레이닝'),
    ('550e8400-e29b-41d4-a716-446655440001', '2023-06-08 17:00:00', '2023-06-08 18:30:00', '산책 및 요가', '한강공원 산책 후 야외 요가', '요가', '한강시민공원'),
    ('550e8400-e29b-41d4-a716-446655440001', '2023-06-11 10:00:00', '2023-06-11 11:30:00', '힐링 명상 요가', '명상과 스트레칭 위주 요가', '요가', '요가스튜디오 서울');

-- 사진 데이터 생성 (운동별로 다양한 개수, 일부는 사진 없음)
INSERT INTO pictures (picture_name, picture_extension, picture_size, picture_url)
VALUES
    -- 첫 번째 회원의 첫 번째 운동 (ID: 1) - 5장
    ('weight_training_1', 'jpg', 1200000, 'https://storage.undabang.com/exercises/weight_back_1.jpg'),
    ('weight_training_2', 'jpg', 980000, 'https://storage.undabang.com/exercises/weight_back_2.jpg'),
    ('weight_training_3', 'jpg', 1050000, 'https://storage.undabang.com/exercises/weight_back_3.jpg'),
    ('weight_training_4', 'jpg', 1120000, 'https://storage.undabang.com/exercises/weight_back_4.jpg'),
    ('weight_training_5', 'jpg', 990000, 'https://storage.undabang.com/exercises/weight_back_5.jpg'),

    -- 첫 번째 회원의 두 번째 운동 (ID: 2) - 0장 (사진 없음)

    -- 첫 번째 회원의 세 번째 운동 (ID: 3) - 3장
    ('leg_day_1', 'jpg', 780000, 'https://storage.undabang.com/exercises/leg_workout_1.jpg'),
    ('leg_day_2', 'jpg', 800000, 'https://storage.undabang.com/exercises/leg_workout_2.jpg'),
    ('leg_day_3', 'jpg', 910000, 'https://storage.undabang.com/exercises/leg_workout_3.jpg'),

    -- 첫 번째 회원의 네 번째 운동 (ID: 4) - 0장 (사진 없음)

    -- 첫 번째 회원의 다섯 번째 운동 (ID: 5) - 2장
    ('chest_workout_1', 'jpg', 1020000, 'https://storage.undabang.com/exercises/chest_workout_1.jpg'),
    ('chest_workout_2', 'jpg', 880000, 'https://storage.undabang.com/exercises/chest_workout_2.jpg'),

    -- 아침 유산소 운동에 대한 사진 1장만 추가
    ('morning_cardio', 'jpg', 820000, 'https://storage.undabang.com/exercises/morning_cardio.jpg'),

    -- 두 번째 회원의 첫 번째 운동 (ID: 6) - 4장
    ('yoga_morning_1', 'jpg', 720000, 'https://storage.undabang.com/exercises/yoga_morning_1.jpg'),
    ('yoga_morning_2', 'jpg', 680000, 'https://storage.undabang.com/exercises/yoga_morning_2.jpg'),
    ('yoga_morning_3', 'jpg', 750000, 'https://storage.undabang.com/exercises/yoga_morning_3.jpg'),
    ('yoga_morning_4', 'jpg', 690000, 'https://storage.undabang.com/exercises/yoga_morning_4.jpg'),

    -- 두 번째 회원의 두 번째 운동 (ID: 7) - 2장
    ('pilates_1', 'jpg', 690000, 'https://storage.undabang.com/exercises/pilates_1.jpg'),
    ('pilates_2', 'jpg', 710000, 'https://storage.undabang.com/exercises/pilates_2.jpg'),

    -- 두 번째 회원의 세 번째 운동 (ID: 8) - 0장 (사진 없음)

    -- 두 번째 회원의 네 번째 운동 (ID: 9) - 1장
    ('outdoor_yoga_1', 'jpg', 940000, 'https://storage.undabang.com/exercises/outdoor_yoga_1.jpg'),

    -- 두 번째 회원의 다섯 번째 운동 (ID: 10) - 3장
    ('meditation_yoga_1', 'jpg', 650000, 'https://storage.undabang.com/exercises/meditation_yoga_1.jpg'),
    ('meditation_yoga_2', 'jpg', 700000, 'https://storage.undabang.com/exercises/meditation_yoga_2.jpg'),
    ('meditation_yoga_3', 'jpg', 680000, 'https://storage.undabang.com/exercises/meditation_yoga_3.jpg');

-- 운동과 사진 연결 (exercise_pictures 테이블)
-- 사진 ID가 1부터 순차적으로 증가한다고 가정
INSERT INTO exercise_pictures (picture_id, exercise_id)
VALUES
    -- 첫 번째 회원의 첫 번째 운동 (ID: 1) - 5장
    (1, 1), (2, 1), (3, 1), (4, 1), (5, 1),

    -- 첫 번째 회원의 세 번째 운동 (ID: 3) - 3장
    (6, 3), (7, 3), (8, 3),

    -- 첫 번째 회원의 다섯 번째 운동 (ID: 5) - 2장
    (9, 5), (10, 5),

    -- 두 번째 회원의 첫 번째 운동 (ID: 6) - 4장
    (11, 6), (12, 6), (13, 6), (14, 6),

    -- 두 번째 회원의 두 번째 운동 (ID: 7) - 2장
    (15, 7), (16, 7),

    -- 두 번째 회원의 네 번째 운동 (ID: 9) - 1장
    (17, 9),

    -- 두 번째 회원의 다섯 번째 운동 (ID: 10) - 3장
    (18, 10), (19, 10), (20, 10),
    (21, 11);