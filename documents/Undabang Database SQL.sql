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
drop table if exists members;
drop table if exists reports;

create table exercise_types
(
    exercise_id              bigint auto_increment
        primary key,
    exercise_name            varchar(50)                        not null,
    exercise_type_created_at datetime default CURRENT_TIMESTAMP not null,
    exercise_type_deleted_at datetime                           null,
    exercise_type_emoji      varchar(10)                        not null
);

INSERT INTO exercise_types (exercise_name, exercise_type_emoji) VALUES
                                                                    ('헬스', '💪'),
                                                                    ('조깅', '🏃'),
                                                                    ('자전거', '🚲'),
                                                                    ('수영', '🏊'),
                                                                    ('요가', '🧘'),
                                                                    ('등산', '⛰️'),
                                                                    ('축구', '⚽'),
                                                                    ('농구', '🏀'),
                                                                    ('테니스', '🎾'),
                                                                    ('배드민턴', '🏸');

create table members
(
    member_id           bigint auto_increment
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
        unique (member_nickname)
);

create table chatrooms
(
    chatroom_id         bigint auto_increment
        primary key,
    sender_id           bigint                             not null,
    receiver_id         bigint                             not null,
    chatroom_created_at datetime default CURRENT_TIMESTAMP not null,
    chatroom_deleted_at datetime                           null,
    constraint FK_cr_receiver
        foreign key (receiver_id) references members (member_id),
    constraint FK_cr_sender
        foreign key (sender_id) references members (member_id)
);

create table chats
(
    chat_id         bigint auto_increment
        primary key,
    chatroom_id     bigint                               not null,
    sender_id       bigint                               not null,
    chat_content    varchar(500)                         not null,
    chat_is_read    tinyint(1) default 0                 not null,
    chat_sended_at  datetime   default CURRENT_TIMESTAMP not null,
    chat_deleted_at datetime                             null,
    constraint FK_c_chatroom
        foreign key (chatroom_id) references chatrooms (chatroom_id),
    constraint FK_c_sender
        foreign key (sender_id) references members (member_id)
);

create table exercises
(
    exercise_id            bigint auto_increment
        primary key,
    member_id              bigint                                                 not null,
    exercise_started_at    datetime default CURRENT_TIMESTAMP                     not null,
    exercise_ended_at      datetime default (exercise_started_at + interval 1 hour) not null,
    exercise_detail        text                                                   not null,
    exercise_title         varchar(255)                                           not null,
    exercise_personal_type varchar(255)                                           not null comment '시스템이 아닌 개인 등록',
    exercise_created_at    datetime default CURRENT_TIMESTAMP                     not null,
    exercise_deleted_at    datetime                                               null,
    constraint FK_ex_member
        foreign key (member_id) references members (member_id)
);

create table member_blocks
(
    member_block_id         bigint auto_increment
        primary key,
    blocker_id              bigint                             not null,
    blocked_id              bigint                             not null,
    member_block_created_at datetime default CURRENT_TIMESTAMP not null,
    member_block_deleted_at datetime                           null,
    constraint FK_mb_blocked
        foreign key (blocked_id) references members (member_id),
    constraint FK_mb_blocker
        foreign key (blocker_id) references members (member_id)
);

create table member_locations
(
    member_location_id         bigint auto_increment
        primary key,
    member_id                  bigint                             not null,
    member_location_title      varchar(255)                       not null comment '사용자 지정 명칭',
    member_location_latitude   varchar(30)                        not null comment '위도(18자)',
    member_location_longitude  varchar(30)                        not null comment '경도(18자)',
    member_location_address    varchar(255)                       not null comment '주소(34자)',
    member_location_created_at datetime default CURRENT_TIMESTAMP not null,
    member_location_deleted_at datetime                           null,
    constraint FK_ml_member
        foreign key (member_id) references members (member_id)
);

CREATE TABLE pictures (
                          picture_id         bigint auto_increment primary key,
                          picture_name       varchar(255)                       null,
                          picture_extension  varchar(10)                        null,
                          picture_size       int                                null comment '바이트 단위',
                          picture_url        varchar(255)                       null,
                          picture_created_at datetime default CURRENT_TIMESTAMP not null,
                          picture_deleted_at datetime                           null
);

create table exercise_pictures
(
    picture_id  bigint not null
        primary key,
    exercise_id bigint not null,
    constraint FK_ep_exercises
        foreign key (exercise_id) references exercises (exercise_id),
    constraint FK_ep_pictures
        foreign key (picture_id) references pictures (picture_id)
);

create table member_pictures
(
    picture_id                 bigint                             not null
        primary key,
    member_id                  bigint                             not null,
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

create table post_type
(
    post_type_id   bigint auto_increment
        primary key,
    post_type_name varchar(255) not null,
    post_type_desc varchar(255) not null
);

INSERT INTO post_type (post_type_name, post_type_desc) VALUES
    ('오운완 게시판', '오늘의 운동한 모습이나 결과를 자랑하는 게시판입니다');

create table posts
(
    post_id          bigint auto_increment
        primary key,
    member_id        bigint                               not null,
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

create table comments
(
    comment_id          bigint auto_increment
        primary key,
    member_id           bigint                               not null,
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

create table likes
(
    like_id          bigint auto_increment
        primary key,
    member_id        bigint                             not null,
    post_id          bigint                             not null,
    like_created_at  datetime default CURRENT_TIMESTAMP not null,
    like_canceled_at datetime                           null,
    constraint FK_likes_member
        foreign key (member_id) references members (member_id),
    constraint FK_likes_post
        foreign key (post_id) references posts (post_id)
);

create table post_pictures
(
    picture_id bigint not null
        primary key,
    post_id    bigint not null,
    constraint FK_pp_pictures
        foreign key (picture_id) references pictures (picture_id),
    constraint FK_pp_posts
        foreign key (post_id) references posts (post_id)
);

create table preferred_exercises
(
    preferred_exercise_id          bigint auto_increment
        primary key,
    exercise_id                     bigint                             not null,
    member_id                       bigint                             not null,
    preferred_exercise_created_at  datetime default CURRENT_TIMESTAMP not null,
    preferred_exercise_deleted_at  datetime                           null,
    preferred_exercise_skill_level varchar(30)                        null,
    constraint check_preferred_exercise_skill_level check (preferred_exercise_skill_level in ('BEGINNER', 'NOVICE', 'INTERMEDIATE', 'EXPERT')),
    constraint FK_pe_member
        foreign key (member_id) references members (member_id),
    constraint FK_pe_type
        foreign key (exercise_id) references exercise_types (exercise_id)
);

create table reports
(
    report_id                 bigint auto_increment
        primary key,
    report_content            varchar(500)                          null,
    report_datetime           datetime    default CURRENT_TIMESTAMP not null,
    report_processing_status  varchar(30) default 'PENDING'         not null,
    report_processed_at       datetime                              null,
    report_processing_content varchar(500)                          null,
    constraint check_report_processing_status check (report_processing_status in ('PENDING', 'PROCESSING', 'COMPLETED', 'REJECTED', 'POSTPONED'))
);

create table comment_report_subjects
(
    comment_report_subject_id   bigint auto_increment       not null
        primary key,
    comment_report_subject_name varchar(255) not null
);

INSERT INTO comment_report_subjects (comment_report_subject_name) VALUES
                                                                      ('스팸홍보/도배입니다.'),
                                                                      ('음란물입니다.'),
                                                                      ('불법정보를 포함하고 있습니다.'),
                                                                      ('청소년에게 유해한 내용입니다.'),
                                                                      ('욕설/생명경시/혐오/차별적 표현입니다.'),
                                                                      ('개인정보가 노출되었습니다.'),
                                                                      ('불쾌한 표현이 있습니다.'),
                                                                      ('기타');

create table comment_reports
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

create table member_report_subjects
(
    member_report_subject_id   bigint auto_increment       not null
        primary key,
    member_report_subject_name varchar(255) not null
);

INSERT INTO member_report_subjects (member_report_subject_name) VALUES
                                                                    ('사용자 사진에 음란물이 있습니다.'),
                                                                    ('사용자 정보에 불법정보를 포함하고 있습니다.'),
                                                                    ('사용자 정보에 청소년에게 유해한 내용이 있습니다.'),
                                                                    ('사용자 정보에 욕설/생명경시/혐오/차별적 표현이 있습니다.'),
                                                                    ('사용자 정보에 개인정보가 노출되었습니다.'),
                                                                    ('사용자 정보에 불쾌한 표현이 있습니다.'),
                                                                    ('약속된 운동에 상습적으로 무단 불참하였습니다.'),
                                                                    ('기타');

create table member_reports
(
    report_id                bigint not null
        primary key,
    member_id                bigint not null,
    member_report_subject_id bigint not null,
    constraint FK_member_report_subjects_TO_member_reports_1
        foreign key (member_report_subject_id) references member_report_subjects (member_report_subject_id),
    constraint FK_members_TO_member_reports_1
        foreign key (member_id) references members (member_id),
    constraint FK_reports_TO_member_reports_1
        foreign key (report_id) references reports (report_id)
);

create table post_report_subjects
(
    post_report_subject_id   bigint auto_increment       not null
        primary key,
    post_report_subject_name varchar(255) not null
);

INSERT INTO post_report_subjects (post_report_subject_name) VALUES
                                                                ('스팸홍보/도배입니다.'),
                                                                ('음란물입니다.'),
                                                                ('불법정보를 포함하고 있습니다.'),
                                                                ('청소년에게 유해한 내용입니다.'),
                                                                ('욕설/생명경시/혐오/차별적 표현입니다.'),
                                                                ('개인정보가 노출되었습니다.'),
                                                                ('불쾌한 표현이 있습니다.'),
                                                                ('기타');

create table post_reports
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