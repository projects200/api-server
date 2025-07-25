drop table if exists BATCH_JOB_EXECUTION_CONTEXT;
drop table if exists BATCH_STEP_EXECUTION_CONTEXT;
drop table if exists BATCH_JOB_EXECUTION_PARAMS;
drop table if exists BATCH_STEP_EXECUTION;
drop table if exists BATCH_JOB_EXECUTION;
drop table if exists BATCH_JOB_INSTANCE;

drop table if exists BATCH_STEP_EXECUTION_SEQ;
drop table if exists BATCH_JOB_EXECUTION_SEQ;
drop table if exists BATCH_JOB_SEQ;

drop table if exists chats;
drop table if exists chatrooms;
drop table if exists comment_reports;
drop table if exists comment_report_subjects;
drop table if exists comments;
drop table if exists exercise_pictures;
drop table if exists exercises;
drop table if exists likes;
drop table if exists member_blocks;
drop table if exists member_locations;
drop table if exists member_pictures;
drop table if exists member_reports;
drop table if exists member_report_subjects;
drop table if exists policy_group_mappings;
drop table if exists policy_groups;
drop table if exists policies;
drop table if exists post_pictures;
drop table if exists pictures;
drop table if exists post_reports;
drop table if exists post_report_subjects;
drop table if exists posts;
drop table if exists post_type;
drop table if exists preferred_exercises;
drop table if exists exercise_types;
drop table if exists members;
drop table if exists reports;

DROP TRIGGER IF EXISTS before_insert_members;

create table if not exists BATCH_JOB_EXECUTION_SEQ
(
    ID         bigint not null,
    UNIQUE_KEY char   not null,
    constraint UNIQUE_KEY_UN
        unique (UNIQUE_KEY)
);

create table if not exists BATCH_JOB_INSTANCE
(
    JOB_INSTANCE_ID bigint       not null
        primary key,
    VERSION         bigint       null,
    JOB_NAME        varchar(100) not null,
    JOB_KEY         varchar(32)  not null,
    constraint JOB_INST_UN
        unique (JOB_NAME, JOB_KEY)
);

create table if not exists BATCH_JOB_EXECUTION
(
    JOB_EXECUTION_ID           bigint        not null
        primary key,
    VERSION                    bigint        null,
    JOB_INSTANCE_ID            bigint        not null,
    CREATE_TIME                datetime(6)   not null,
    START_TIME                 datetime(6)   null,
    END_TIME                   datetime(6)   null,
    STATUS                     varchar(10)   null,
    EXIT_CODE                  varchar(2500) null,
    EXIT_MESSAGE               varchar(2500) null,
    LAST_UPDATED               datetime(6)   null,
    JOB_CONFIGURATION_LOCATION varchar(2500) null,
    constraint JOB_INST_EXEC_FK
        foreign key (JOB_INSTANCE_ID) references BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
);

create table if not exists BATCH_JOB_EXECUTION_CONTEXT
(
    JOB_EXECUTION_ID   bigint        not null
        primary key,
    SHORT_CONTEXT      varchar(2500) not null,
    SERIALIZED_CONTEXT text          null,
    constraint JOB_EXEC_CTX_FK
        foreign key (JOB_EXECUTION_ID) references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

create table if not exists BATCH_JOB_EXECUTION_PARAMS
(
    JOB_EXECUTION_ID bigint       not null,
    TYPE_CD          varchar(6)   not null,
    KEY_NAME         varchar(100) not null,
    STRING_VAL       varchar(250) null,
    DATE_VAL         datetime(6)  null,
    LONG_VAL         bigint       null,
    DOUBLE_VAL       double       null,
    IDENTIFYING      char         not null,
    constraint JOB_EXEC_PARAMS_FK
        foreign key (JOB_EXECUTION_ID) references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

create table if not exists BATCH_JOB_SEQ
(
    ID         bigint not null,
    UNIQUE_KEY char   not null,
    constraint UNIQUE_KEY_UN
        unique (UNIQUE_KEY)
);

create table if not exists BATCH_STEP_EXECUTION
(
    STEP_EXECUTION_ID  bigint        not null
        primary key,
    VERSION            bigint        not null,
    STEP_NAME          varchar(100)  not null,
    JOB_EXECUTION_ID   bigint        not null,
    START_TIME         datetime(6)   not null,
    END_TIME           datetime(6)   null,
    STATUS             varchar(10)   null,
    COMMIT_COUNT       bigint        null,
    READ_COUNT         bigint        null,
    FILTER_COUNT       bigint        null,
    WRITE_COUNT        bigint        null,
    READ_SKIP_COUNT    bigint        null,
    WRITE_SKIP_COUNT   bigint        null,
    PROCESS_SKIP_COUNT bigint        null,
    ROLLBACK_COUNT     bigint        null,
    EXIT_CODE          varchar(2500) null,
    EXIT_MESSAGE       varchar(2500) null,
    LAST_UPDATED       datetime(6)   null,
    constraint JOB_EXEC_STEP_FK
        foreign key (JOB_EXECUTION_ID) references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

create table if not exists BATCH_STEP_EXECUTION_CONTEXT
(
    STEP_EXECUTION_ID  bigint        not null
        primary key,
    SHORT_CONTEXT      varchar(2500) not null,
    SERIALIZED_CONTEXT text          null,
    constraint STEP_EXEC_CTX_FK
        foreign key (STEP_EXECUTION_ID) references BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
);

create table if not exists BATCH_STEP_EXECUTION_SEQ
(
    ID         bigint not null,
    UNIQUE_KEY char   not null,
    constraint UNIQUE_KEY_UN
        unique (UNIQUE_KEY)
);

create table if not exists comment_report_subjects
(
    comment_report_subject_id   bigint auto_increment
        primary key,
    comment_report_subject_name varchar(255) not null
);

create table if not exists exercise_types
(
    exercise_id              bigint auto_increment
        primary key,
    exercise_name            varchar(50)                        not null,
    exercise_type_created_at datetime default CURRENT_TIMESTAMP not null,
    exercise_type_deleted_at datetime                           null,
    exercise_type_emoji      varchar(10)                        not null
);

create table if not exists member_report_subjects
(
    member_report_subject_id   bigint auto_increment
        primary key,
    member_report_subject_name varchar(255) not null
);

create table if not exists members
(
    member_id           char(36)                           not null
        primary key,
    member_email        varchar(320)                       null,
    member_gender       char                               null comment 'M: 남 / F: 여 / U: 비공개',
    member_bday         date                               null,
    member_nickname     varchar(50)                        not null,
    member_desc         varchar(500)                       null,
    member_score tinyint not null comment '0~100',
    member_warned_count tinyint  default 0                 not null comment '관리자 처리 신고 누적',
    member_created_at   datetime default CURRENT_TIMESTAMP not null,
    member_deleted_at   datetime                           null comment '탈퇴 시 삭제 일시 기록',
    constraint member_email
        unique (member_email),
    constraint member_nickname
        unique (member_nickname),
    constraint check_member_gender
        check (`member_gender` in ('M','F','U'))
);

create table if not exists chatrooms
(
    chatroom_id         bigint auto_increment
        primary key,
    sender_id           char(36)                           not null,
    receiver_id         char(36)                           not null,
    chatroom_created_at datetime default CURRENT_TIMESTAMP not null,
    chatroom_deleted_at datetime                           null,
    constraint FK_cr_receiver
        foreign key (receiver_id) references members (member_id),
    constraint FK_cr_sender
        foreign key (sender_id) references members (member_id)
);

create table if not exists chats
(
    chat_id         bigint auto_increment
        primary key,
    chatroom_id     bigint                               not null,
    sender_id       char(36)                             not null,
    chat_content    varchar(500)                         not null,
    chat_is_read    tinyint(1) default 0                 not null,
    chat_sended_at  datetime   default CURRENT_TIMESTAMP not null,
    chat_deleted_at datetime                             null,
    constraint FK_c_chatroom
        foreign key (chatroom_id) references chatrooms (chatroom_id),
    constraint FK_c_sender
        foreign key (sender_id) references members (member_id)
);

create table if not exists exercises
(
    exercise_id            bigint auto_increment
        primary key,
    member_id              char(36)                                                     not null,
    exercise_started_at    datetime default CURRENT_TIMESTAMP                           not null,
    exercise_ended_at      datetime default ((`exercise_started_at` + interval 1 hour)) not null,
    exercise_detail        text                                                         null,
    exercise_title         varchar(255)                                                 not null,
    exercise_personal_type varchar(255)                                                 null comment '시스템이 아닌 개인 등록',
    exercise_created_at    datetime default CURRENT_TIMESTAMP                           not null,
    exercise_deleted_at    datetime                                                     null,
    exercise_location      varchar(255)                                                 null,
    constraint FK_ex_member
        foreign key (member_id) references members (member_id)
);

create table if not exists member_blocks
(
    member_block_id         bigint auto_increment
        primary key,
    blocker_id              char(36)                           not null,
    blocked_id              char(36)                           not null,
    member_block_created_at datetime default CURRENT_TIMESTAMP not null,
    member_block_deleted_at datetime                           null,
    constraint FK_mb_blocked
        foreign key (blocked_id) references members (member_id),
    constraint FK_mb_blocker
        foreign key (blocker_id) references members (member_id)
);

create table if not exists member_locations
(
    member_location_id         bigint auto_increment
        primary key,
    member_id                  char(36)                           not null,
    member_location_title      varchar(255)                       not null comment '사용자 지정 명칭',
    member_location_latitude   varchar(30)                        not null comment '위도(18자)',
    member_location_longitude  varchar(30)                        not null comment '경도(18자)',
    member_location_address    varchar(255)                       not null comment '주소(34자)',
    member_location_created_at datetime default CURRENT_TIMESTAMP not null,
    member_location_deleted_at datetime                           null,
    constraint FK_ml_member
        foreign key (member_id) references members (member_id)
);

create table if not exists pictures
(
    picture_id         bigint auto_increment
        primary key,
    picture_name       varchar(255)                       null,
    picture_extension  varchar(10)                        null,
    picture_size       int                                null comment '바이트 단위',
    picture_url        varchar(255)                       null,
    picture_created_at datetime default CURRENT_TIMESTAMP not null,
    picture_deleted_at datetime                           null
);

create table if not exists exercise_pictures
(
    picture_id  bigint not null
        primary key,
    exercise_id bigint not null,
    constraint FK_ep_exercises
        foreign key (exercise_id) references exercises (exercise_id),
    constraint FK_ep_pictures
        foreign key (picture_id) references pictures (picture_id)
);

create table if not exists member_pictures
(
    picture_id                 bigint                             not null
        primary key,
    member_id                  char(36)                           not null,
    member_pictures_name       varchar(255)                       null,
    member_pictures_size       int                                null comment '바이트 단위',
    member_pictures_url        varchar(255)                       null,
    member_pictures_created_at datetime default CURRENT_TIMESTAMP not null,
    member_pictures_deleted_at datetime                           null,
    constraint FK_mp_member
        foreign key (member_id) references members (member_id),
    constraint FK_mp_pictures
        foreign key (picture_id) references pictures (picture_id)
);

create table if not exists policies (
    policy_id             int             not null    auto_increment primary key,
    policy_key            varchar(100)    not null    unique comment '정책을 식별하는 키 (ex:SCORE_INITIAL)',
    policy_value          varchar(255)    null comment '정책 값',
    policy_unit           varchar(20)     null comment '정책 값의 단위',
    policy_description    varchar(500)    null comment '관리자 페이지에 표시될 정책 설명',
    policy_updated_at     datetime        not null default current_timestamp comment '마지막 수정 일시',
    policy_created_at     datetime        not null default current_timestamp comment '생성일시'
);

create table if not exists policy_groups (
    policy_groups_id            int             not null    auto_increment primary key,
    policy_groups_name          varchar(100)    not null    unique comment '정책 타입 이름',
    policy_groups_created_at    datetime        not null    default current_timestamp comment '생성일시',
    policy_groups_updated_at    datetime        not null    default current_timestamp comment '수정일시'
);

create table if not exists policy_group_mappings (
    mapping_id                  int             not null    auto_increment primary key comment '정책 타입 매핑 id',
    policy_id                 int             not null    comment '정책번호',
    policy_groups_id             int             not null    comment '정책 그룹 번호',
    foreign key (policy_id) references policies(policy_id),
    foreign key (policy_groups_id) references policy_groups(policy_groups_id)
);

create table if not exists post_report_subjects
(
    post_report_subject_id   bigint auto_increment
        primary key,
    post_report_subject_name varchar(255) not null
);

create table if not exists post_type
(
    post_type_id   bigint auto_increment
        primary key,
    post_type_name varchar(255) not null,
    post_type_desc varchar(255) not null
);

create table if not exists posts
(
    post_id          bigint auto_increment
        primary key,
    member_id        char(36)                             not null,
    post_type_id     bigint                               not null,
    post_content     text                                 not null,
    post_is_reported tinyint(1) default 0                 null comment '관리자 제제 시 1',
    post_created_at  datetime   default CURRENT_TIMESTAMP not null,
    post_deleted_at  datetime                             null,
    post_likes_cnt   int        default 0                 not null,
    constraint FK_posts_member
        foreign key (member_id) references members (member_id),
    constraint FK_posts_type
        foreign key (post_type_id) references post_type (post_type_id)
);

create table if not exists comments
(
    comment_id          bigint auto_increment
        primary key,
    member_id           char(36)                             not null,
    post_id             bigint                               not null,
    comment_content     varchar(255)                         not null,
    comment_is_reported tinyint(1) default 0                 null comment '관리자 제제 시 1',
    comment_created_at  datetime   default CURRENT_TIMESTAMP not null,
    comment_deleted_at  datetime                             null,
    constraint FK_comments_member
        foreign key (member_id) references members (member_id),
    constraint FK_comments_post
        foreign key (post_id) references posts (post_id)
);

create table if not exists likes
(
    like_id          bigint auto_increment
        primary key,
    member_id        char(36)                           not null,
    post_id          bigint                             not null,
    like_created_at  datetime default CURRENT_TIMESTAMP not null,
    like_canceled_at datetime                           null,
    constraint FK_likes_member
        foreign key (member_id) references members (member_id),
    constraint FK_likes_post
        foreign key (post_id) references posts (post_id)
);

create table if not exists post_pictures
(
    picture_id bigint not null
        primary key,
    post_id    bigint not null,
    constraint FK_pp_pictures
        foreign key (picture_id) references pictures (picture_id),
    constraint FK_pp_posts
        foreign key (post_id) references posts (post_id)
);

create table if not exists preferred_exercises
(
    preferred_exercise_id          bigint auto_increment
        primary key,
    exercise_id                    bigint                             not null,
    member_id                      char(36)                           not null,
    preferred_exercise_created_at  datetime default CURRENT_TIMESTAMP not null,
    preferred_exercise_deleted_at  datetime                           null,
    preferred_exercise_skill_level varchar(30)                        null,
    constraint FK_pe_member
        foreign key (member_id) references members (member_id),
    constraint FK_pe_type
        foreign key (exercise_id) references exercise_types (exercise_id),
    constraint check_preferred_exercise_skill_level
        check (`preferred_exercise_skill_level` in ('BEGINNER','NOVICE','INTERMEDIATE','EXPERT'))
);

create table if not exists reports
(
    report_id                 bigint auto_increment
        primary key,
    report_content            varchar(500)                          null,
    report_datetime           datetime    default CURRENT_TIMESTAMP not null,
    report_processing_status  varchar(30) default 'PENDING'         not null,
    report_processed_at       datetime                              null,
    report_processing_content varchar(500)                          null,
    constraint check_report_processing_status
        check (`report_processing_status` in ('PENDING','PROCESSING','COMPLETED','REJECTED','POSTPONED'))
);

create table if not exists comment_reports
(
    report_id                 bigint not null
        primary key,
    comment_id                bigint not null,
    comment_report_subject_id bigint not null,
    constraint FK_comment_report_subjects_TO_comment_reports_1
        foreign key (comment_report_subject_id) references comment_report_subjects (comment_report_subject_id),
    constraint FK_comments_TO_comment_reports_1
        foreign key (comment_id) references comments (comment_id),
    constraint FK_reports_TO_comment_reports_1
        foreign key (report_id) references reports (report_id)
);

create table if not exists member_reports
(
    report_id                bigint   not null
        primary key,
    member_id                char(36) not null,
    member_report_subject_id bigint   not null,
    constraint FK_member_report_subjects_TO_member_reports_1
        foreign key (member_report_subject_id) references member_report_subjects (member_report_subject_id),
    constraint FK_members_TO_member_reports_1
        foreign key (member_id) references members (member_id),
    constraint FK_reports_TO_member_reports_1
        foreign key (report_id) references reports (report_id)
);

create table if not exists post_reports
(
    report_id              bigint not null
        primary key,
    post_id                bigint not null,
    post_report_subject_id bigint not null,
    constraint FK_post_report_subjects_TO_post_reports_1
        foreign key (post_report_subject_id) references post_report_subjects (post_report_subject_id),
    constraint FK_posts_TO_post_reports_1
        foreign key (post_id) references posts (post_id),
    constraint FK_reports_TO_post_reports_1
        foreign key (report_id) references reports (report_id)
);

CREATE TRIGGER before_insert_members
    BEFORE INSERT
    ON members
    FOR EACH ROW
BEGIN
    IF NEW.member_id IS NULL OR NEW.member_id = '' THEN
        SET NEW.member_id = UUID();
    END IF;
END;

INSERT INTO exercise_types (exercise_name, exercise_type_emoji)
VALUES ('헬스', '💪'),
       ('조깅', '🏃'),
       ('자전거', '🚲'),
       ('수영', '🏊'),
       ('요가', '🧘'),
       ('등산', '⛰️'),
       ('축구', '⚽'),
       ('농구', '🏀'),
       ('테니스', '🎾'),
       ('배드민턴', '🏸');

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

INSERT INTO policies (policy_key, policy_value, policy_unit, policy_description)
VALUES
    -- 점수 범위 정책
    ('EXERCISE_SCORE_MAX_POINTS', '100', 'POINTS', '회원이 가질 수 있는 최대 운동 점수'),
    ('EXERCISE_SCORE_MIN_POINTS', '0', 'POINTS', '회원이 가질 수 있는 최소 운동 점수'),

    -- 점수 획득 정책
    ('SIGNUP_INITIAL_POINTS', '35', 'POINTS', '회원 가입 시 기본으로 부여되는 점수'),
    ('POINTS_PER_EXERCISE', '3', 'POINTS', '운동 기록 1회당 부여되는 점수 (일 1회)'),
    ('EXERCISE_RECORD_VALIDITY_PERIOD', '2', 'DAYS', '점수 획득이 가능한 운동 기록의 유효 기간. (단위: DAYS, HOURS, MINUTES)'),
    ('EXERCISE_RECORD_MAX_PER_DAY', '1', 'COUNT', '하루에 기록할 수 있는 최대 운동 횟수'),

    -- 점수 차감 (페널티) 정책
    ('PENALTY_INACTIVITY_THRESHOLD_DAYS', '7', 'DAYS', '페널티가 시작되는 비활성 기준일 (이 기간 이상 운동 기록이 없을 경우)'),
    ('PENALTY_SCORE_DECREMENT_POINTS', '1', 'POINTS', '비활성 상태일 때 매일 차감되는 점수');

INSERT INTO policy_groups (policy_groups_name) VALUES ('exercise-score');

INSERT INTO policy_group_mappings (policy_id, policy_groups_id)
SELECT
    p.policy_id, -- (A) 조회된 각 정책의 ID
    (SELECT pg.policy_groups_id FROM policy_groups pg WHERE pg.policy_groups_name = 'exercise-score') -- (B) 'exercise-score' 그룹의 ID
FROM
    policies p
WHERE
    p.policy_key IN (
                       'EXERCISE_SCORE_MAX_POINTS',
                       'EXERCISE_SCORE_MIN_POINTS',
                       'SIGNUP_INITIAL_POINTS',
                       'POINTS_PER_EXERCISE',
                       'EXERCISE_RECORD_VALIDITY_PERIOD',
                       'EXERCISE_RECORD_MAX_PER_DAY',
                       'PENALTY_INACTIVITY_THRESHOLD_DAYS',
                       'PENALTY_SCORE_DECREMENT_POINTS'
        )
  -- 이미 매핑된 데이터는 중복 삽입하지 않도록 방지하는 로직
  AND NOT EXISTS (
    SELECT 1 FROM policy_group_mappings pgm
    WHERE pgm.policy_id = p.policy_id
      AND pgm.policy_groups_id = (SELECT pg.policy_groups_id FROM policy_groups pg WHERE pg.policy_groups_name = 'exercise-score')
);