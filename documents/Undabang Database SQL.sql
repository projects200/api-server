drop table if exists BATCH_JOB_EXECUTION_CONTEXT;
drop table if exists BATCH_STEP_EXECUTION_CONTEXT;
drop table if exists BATCH_JOB_EXECUTION_PARAMS;
drop table if exists BATCH_STEP_EXECUTION;
drop table if exists BATCH_JOB_EXECUTION;
drop table if exists BATCH_JOB_INSTANCE;

drop table if exists BATCH_STEP_EXECUTION_SEQ;
drop table if exists BATCH_JOB_EXECUTION_SEQ;
drop table if exists BATCH_JOB_SEQ;

drop table if exists policy_group_mappings;
drop table if exists policy_groups;
drop table if exists policies;

DROP TABLE IF EXISTS fcm_tokens;
DROP TABLE IF EXISTS scenario_message_mappings;
DROP TABLE IF EXISTS notification_messages;
DROP TABLE IF EXISTS notification_scenarios;

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
drop table if exists post_pictures;
drop table if exists pictures;
drop table if exists post_reports;
drop table if exists post_report_subjects;
drop table if exists posts;
drop table if exists post_type;
drop table if exists preferred_exercises;
drop table if exists exercise_types;
drop table if exists custom_timer_steps;
drop table if exists simple_timers;
drop table if exists custom_timers;
drop table if exists members;
drop table if exists reports;

DROP TRIGGER IF EXISTS before_insert_members;

create table if not exists BATCH_JOB_EXECUTION_SEQ
(
    ID         bigint not null,
    UNIQUE_KEY char   not null,
    constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
);


create table if not exists BATCH_JOB_INSTANCE
(
    JOB_INSTANCE_ID bigint not null primary key,
    VERSION         bigint       null,
    JOB_NAME        varchar(100) not null,
    JOB_KEY         varchar(32)  not null,
    constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
);

create table if not exists BATCH_JOB_EXECUTION
(
    JOB_EXECUTION_ID bigint not null primary key,
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
    constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID) references BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
);

create table if not exists BATCH_JOB_EXECUTION_CONTEXT
(
    JOB_EXECUTION_ID   bigint        not null primary key,
    SHORT_CONTEXT      varchar(2500) not null,
    SERIALIZED_CONTEXT text          null,
    constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID) references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

create table if not exists BATCH_JOB_EXECUTION_PARAMS
(
    JOB_EXECUTION_ID bigint        not null,
    PARAMETER_NAME   varchar(100)  not null,
    PARAMETER_TYPE   varchar(100)  not null,
    PARAMETER_VALUE  varchar(2500) null,
    IDENTIFYING      char(1)       not null,
    constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID) references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

create table if not exists BATCH_JOB_SEQ
(
    ID         bigint not null,
    UNIQUE_KEY char   not null,
    constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
);


create table if not exists BATCH_STEP_EXECUTION
(
    STEP_EXECUTION_ID  bigint        not null primary key,
    VERSION            bigint        not null,
    STEP_NAME          varchar(100)  not null,
    JOB_EXECUTION_ID   bigint        not null,
    CREATE_TIME        datetime(6)   not null, -- 5.x 버전에서 CREATE_TIME 추가
    START_TIME         datetime(6)   null,     -- START_TIME은 nullable로 변경
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
    constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID) references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

create table if not exists BATCH_STEP_EXECUTION_CONTEXT
(
    STEP_EXECUTION_ID  bigint not null primary key,
    SHORT_CONTEXT      varchar(2500) not null,
    SERIALIZED_CONTEXT text   null,
    constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID) references BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
);

create table if not exists BATCH_STEP_EXECUTION_SEQ
(
    ID         bigint not null,
    UNIQUE_KEY char   not null,
    constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
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
        check (member_gender in ('M', 'F', 'U'))
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
    member_id              char(36)                                                   not null,
    exercise_started_at    datetime default CURRENT_TIMESTAMP                         not null,
    exercise_ended_at      datetime default ((exercise_started_at + interval 1 hour)) not null,
    exercise_detail        text                                                       null,
    exercise_title         varchar(255)                                               not null,
    exercise_personal_type varchar(255)                                               null comment '시스템이 아닌 개인 등록',
    exercise_created_at    datetime default CURRENT_TIMESTAMP                         not null,
    exercise_deleted_at    datetime                                                   null,
    exercise_location      varchar(255)                                               null,
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

create table if not exists policies
(
    policy_id          int          not null auto_increment primary key,
    policy_key         varchar(100) not null unique comment '정책을 식별하는 키 (ex:SCORE_INITIAL)',
    policy_value       varchar(255) null comment '정책 값',
    policy_unit        varchar(20)  null comment '정책 값의 단위',
    policy_description varchar(500) null comment '관리자 페이지에 표시될 정책 설명',
    policy_updated_at  datetime     not null default current_timestamp comment '마지막 수정 일시',
    policy_created_at  datetime     not null default current_timestamp comment '생성일시'
);

create table if not exists policy_groups
(
    policy_groups_id         int          not null auto_increment primary key,
    policy_groups_name       varchar(100) not null unique comment '정책 타입 이름',
    policy_groups_created_at datetime     not null default current_timestamp comment '생성일시',
    policy_groups_updated_at datetime     not null default current_timestamp comment '수정일시'
);

create table if not exists policy_group_mappings
(
    mapping_id       int not null auto_increment primary key comment '정책 타입 매핑 id',
    policy_id        int not null comment '정책번호',
    policy_groups_id int not null comment '정책 그룹 번호',
    foreign key (policy_id) references policies (policy_id),
    foreign key (policy_groups_id) references policy_groups (policy_groups_id)
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
        check (preferred_exercise_skill_level in ('BEGINNER', 'NOVICE', 'INTERMEDIATE', 'EXPERT'))
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
        check (report_processing_status in ('PENDING', 'PROCESSING', 'COMPLETED', 'REJECTED', 'POSTPONED'))
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

create table if not exists custom_timers
(
    custom_timer_id         bigint       not null auto_increment primary key,
    member_id               char(36)     not null comment 'UUID_SELF',
    custom_timer_name       varchar(100) null comment '커스텀 타이머 이름',
    custom_timer_created_at datetime     not null default current_timestamp,
    custom_timer_updated_at datetime     not null default current_timestamp ON UPDATE CURRENT_TIMESTAMP,
    custom_timer_deleted_at datetime     null,
    foreign key (member_id) references members (member_id)
);

create table if not exists simple_timers
(
    simple_timer_id         bigint   not null auto_increment primary key,
    member_id               char(36) not null comment 'UUID_SELF',
    simple_timer_time       int      null comment '심플 타이머 시간',
    simple_timer_created_at datetime not null default current_timestamp,
    simple_timer_updated_at datetime not null default current_timestamp ON UPDATE CURRENT_TIMESTAMP,
    simple_timer_deleted_at datetime null,
    foreign key (member_id) references members (member_id)
);

create table if not exists custom_timer_steps
(
    custom_timer_steps_id         bigint      not null auto_increment primary key,
    custom_timer_id               bigint      not null comment 'AUTO_INCREMENT',
    custom_timer_steps_name varchar(50) null comment '스텝 이름',
    custom_timer_steps_order      tinyint     not null comment '스텝 순서',
    custom_timer_steps_time       int         not null comment '스텝 시간',
    custom_timer_steps_created_at datetime    not null default current_timestamp,
    custom_timer_steps_updated_at datetime    not null default current_timestamp ON UPDATE CURRENT_TIMESTAMP,
    custom_timer_steps_deleted_at datetime    null,
    foreign key (custom_timer_id) references custom_timers (custom_timer_id)
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

CREATE TABLE IF NOT EXISTS fcm_tokens
(
    fcm_token_id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'AUTO_INCREMENT',
    member_id              CHAR(36)     NOT NULL COMMENT 'UUID_SELF',
    fcm_token_value        VARCHAR(255) NOT NULL COMMENT 'FCM 토큰 값, unique',
    fcm_token_user_agent   VARCHAR(255) NULL COMMENT '디바이스 정보 (User Agent)',
    fcm_token_is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '토큰 활성화 여부',
    fcm_token_activated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 활성 일시',
    fcm_token_created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    fcm_token_deleted_at   DATETIME     NULL COMMENT '삭제일시',

    UNIQUE KEY uk_fcm_token_value (fcm_token_value),
    CONSTRAINT fk_fcm_tokens_to_members FOREIGN KEY (member_id) REFERENCES members (member_id)
);

CREATE TABLE IF NOT EXISTS notification_messages
(
    message_id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '메시지 ID',
    message_title      VARCHAR(100)  NULL COMMENT '알림 제목',
    message_body       VARCHAR(1000) NOT NULL COMMENT '알림 본문',
    message_image_url  VARCHAR(255)  NULL COMMENT '알림 이미지 URL',
    message_link_url   VARCHAR(255)  NULL COMMENT '알림 클릭 시 이동할 URL',
    message_created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    message_updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    message_deleted_at DATETIME      NULL COMMENT '삭제일시'
);

CREATE TABLE IF NOT EXISTS notification_scenarios
(
    scenario_id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '시나리오 ID',
    scenario_code        VARCHAR(50)  NOT NULL COMMENT '시나리오 코드 (애플리케이션에서 사용)',
    scenario_description VARCHAR(255) NOT NULL COMMENT '시나리오 설명',
    scenario_is_enabled  BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '시나리오 활성화 여부',
    scenario_created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    scenario_updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    scenario_deleted_at  DATETIME     NULL COMMENT '삭제일시'
);

CREATE TABLE IF NOT EXISTS scenario_message_mappings
(
    mapping_id  BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL COMMENT '시나리오 메시지 매핑 ID',
    message_id  BIGINT                            NOT NULL COMMENT '메시지 ID',
    scenario_id BIGINT                            NOT NULL COMMENT '시나리오 ID',

    CONSTRAINT fk_map_to_messages FOREIGN KEY (message_id) REFERENCES notification_messages (message_id),
    CONSTRAINT fk_map_to_scenarios FOREIGN KEY (scenario_id) REFERENCES notification_scenarios (scenario_id)
);

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