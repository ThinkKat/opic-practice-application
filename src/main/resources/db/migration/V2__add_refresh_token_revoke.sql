-- Migration: refresh_token soft delete 지원
-- 적용일: 2026-03-01
-- 배경: 탈취 감지를 위해 revoke 이력 보관 (hard delete → soft delete 전환)
--       revoke 후 90일 보관, 이후 스케줄러가 삭제

ALTER TABLE refresh_token
    ADD COLUMN revoked    BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN revoked_at TIMESTAMP;
