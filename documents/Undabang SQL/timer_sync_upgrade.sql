-- #574 / #575 기존 데이터베이스용 1회성 추가 마이그레이션입니다.
-- 배포 순서: 이 스키마 적용 완료 -> 두 타이머 기능을 포함한 서버 배포 -> Android #588 배포.
-- 신규 설치용 Undabang Database DDL.sql은 DROP TABLE을 포함하므로 기존 DB에 실행하지 않습니다.
-- MySQL DDL은 암묵적으로 커밋합니다. 각 문장의 적용 상태를 확인한 뒤 서버를 배포합니다.
-- 기존 행과 서버 시각을 보존하며 edited_at은 NULL로 둡니다. 기기 시각을 추측하여 채우지 않습니다.
-- updated_at의 ON UPDATE 속성을 제거합니다. 이후 갱신 시각은 애플리케이션이 관리합니다.
-- 기존 생성/삭제 시각 컬럼은 변경하지 않습니다.
ALTER TABLE simple_timers
    ADD COLUMN edited_at BIGINT NULL COMMENT '기기 수정 시각 UTC Unix epoch milliseconds',
    MODIFY COLUMN simple_timer_updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3);

ALTER TABLE custom_timers
    ADD COLUMN edited_at BIGINT NULL COMMENT '기기 수정 시각 UTC Unix epoch milliseconds',
    MODIFY COLUMN custom_timer_updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3);

-- 타이머 FK나 TTL을 두지 않습니다. 타이머 삭제/정리 이후에도 원래 응답이 필요합니다.
-- 영수증은 타이머 변경과 동일 트랜잭션으로 저장하며 회원 물리 삭제 시에만 CASCADE 삭제합니다.
CREATE TABLE IF NOT EXISTS timer_create_receipts
(
    timer_create_receipt_id BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id               CHAR(36)    NOT NULL,
    create_request_id       CHAR(36)    NOT NULL,
    timer_type              VARCHAR(10) NOT NULL,
    timer_id                BIGINT      NOT NULL,
    updated_at              DATETIME(3) NOT NULL,
    CONSTRAINT uk_timer_create_receipt_member_request UNIQUE (member_id, create_request_id),
    CONSTRAINT fk_timer_create_receipt_member FOREIGN KEY (member_id)
        REFERENCES members (member_id) ON DELETE CASCADE,
    CONSTRAINT chk_timer_create_receipt_type CHECK (timer_type IN ('SIMPLE', 'CUSTOM'))
);

CREATE TABLE IF NOT EXISTS timer_edit_receipts
(
    timer_edit_receipt_id BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id             CHAR(36)    NOT NULL,
    timer_type            VARCHAR(10) NOT NULL,
    timer_id              BIGINT      NOT NULL,
    edit_id               CHAR(36)    NOT NULL,
    operation             VARCHAR(10) NOT NULL,
    updated_at            DATETIME(3) NOT NULL,
    CONSTRAINT uk_timer_edit_receipt_request UNIQUE (member_id, timer_type, timer_id, edit_id),
    CONSTRAINT fk_timer_edit_receipt_member FOREIGN KEY (member_id)
        REFERENCES members (member_id) ON DELETE CASCADE,
    CONSTRAINT chk_timer_edit_receipt_type CHECK (timer_type IN ('SIMPLE', 'CUSTOM')),
    CONSTRAINT chk_timer_edit_receipt_operation CHECK (operation IN ('UPDATE', 'RENAME', 'REPLACE', 'DELETE'))
);
