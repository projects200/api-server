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
    member_score        tinyint  default 35                null comment '0~100, 초기값 35',
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

create definer = admin@`%` trigger before_insert_members
    before insert
    on members
    for each row
BEGIN
    IF NEW.member_id IS NULL OR NEW.member_id = '' THEN
        SET NEW.member_id = UUID();
END IF;
END;

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
    policy_key         varchar(100)                       not null comment '정책을 식별하는 고유 키 (예: SCORE_INITIAL)'
        primary key,
    policy_value       varchar(255)                       not null comment '정책 값',
    policy_unit        varchar(20)                        null comment '정책 값의 단위 (예: POINTS, DAYS)',
    policy_description varchar(500)                       not null comment '관리자 페이지에 표시될 정책 설명',
    policy_updated_at  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '마지막 수정 일시'
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

