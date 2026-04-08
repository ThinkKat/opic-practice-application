-- V004: users.email NOT NULL 제약 추가
-- 배경: 로그인 식별자를 username → email로 변경함에 따라 email 필수화
-- 적용 전 확인: SELECT email FROM users WHERE email IS NULL; → 0 rows 확인됨 (2026-03-30)

ALTER TABLE users ALTER COLUMN email SET NOT NULL;
