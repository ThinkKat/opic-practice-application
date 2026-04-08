-- V008: answers, drill_answers 테이블에 status_changed_at 컬럼 추가
-- 목적: 피드백 상태 변경 시각 추적 (30초 SLA 기준, 스케줄러 타임아웃 판단)

ALTER TABLE answers
    ADD COLUMN status_changed_at TIMESTAMP;

ALTER TABLE drill_answers
    ADD COLUMN status_changed_at TIMESTAMP;
