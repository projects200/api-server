-- 운동 오프라인 동기화: 기존 데이터를 보존하는 MySQL 업그레이드입니다.
-- 운영자는 백업 후 이 SQL을 애플리케이션 배포 전에 한 번 적용합니다.
-- 기존 fresh-install DDL(DROP TABLE 포함)은 운영 업그레이드에 사용하지 않습니다.
-- 기존 운동의 초기 버전은 0입니다. 영수증에는 운동/사진 FK 및 자동 만료를 두지 않습니다.
ALTER TABLE exercises ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS exercise_receipts
(
    exercise_receipt_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id CHAR(36) NOT NULL,
    request_id CHAR(36) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    exercise_id BIGINT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    response_json TEXT NOT NULL,
    CONSTRAINT uk_exercise_receipt_member_request UNIQUE (member_id, request_id),
    CONSTRAINT FK_exercise_receipt_member FOREIGN KEY (member_id) REFERENCES members (member_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS exercise_photo_cleanup
(
    exercise_photo_cleanup_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    INDEX idx_exercise_cleanup_due (next_attempt_at)
);

-- 영수증은 회원 물리 삭제 시에만 함께 삭제합니다. 기록/사진 soft delete 뒤에도 원본 응답을 재생합니다.
-- 정리 작업에는 회원 FK가 없습니다. 회원 삭제 뒤에도 이미 확정된 S3 삭제를 재시도해야 합니다.
