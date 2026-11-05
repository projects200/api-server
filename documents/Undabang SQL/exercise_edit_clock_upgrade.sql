-- 운동 본문 편집 시각: exercise_sync_upgrade.sql 적용 이후의 MySQL 업그레이드입니다.
-- 운영자는 백업 후 애플리케이션 배포 전에 한 번 적용합니다. 초기 설치 DDL은 사용하지 않습니다.
-- 기존 행은 NULL(순서 불명)로 남깁니다. 생성/수정/영수증 시각으로 과거 편집 시각을 만들지 않습니다.
-- 기존 본문, 사진, 버전, 삭제 상태와 성공 영수증은 변경하지 않습니다.
ALTER TABLE exercises ADD COLUMN edited_at BIGINT NULL;
