-- OPIc 연습 앱 Database Schema (PostgreSQL)
-- 생성일: 2026-02-02
-- 버전: 1.0.0 (MVP)

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    email TEXT UNIQUE,
    terms_agreed_at   TIMESTAMP,
    privacy_agreed_at TIMESTAMP,
    user_role_code CHAR(7) NOT NULL DEFAULT 'USR0001',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE question_type (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    question_type_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    audio_file_url TEXT,
    duration_ms INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE question_sets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE question_set_items (
    item_id BIGSERIAL PRIMARY KEY,
    question_set_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    order_index INTEGER NOT NULL,
    UNIQUE(question_set_id, question_id),
    UNIQUE(question_set_id, order_index)
);

CREATE TABLE session_status_mapping (
    status_code CHAR(7) PRIMARY KEY,
    status_text TEXT NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_set_id BIGINT,
    title TEXT NOT NULL,
    mode TEXT NOT NULL,
    status_code CHAR(7) NOT NULL,
    current_index INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    audio_url TEXT NOT NULL,
    storage_type VARCHAR(10) NOT NULL,
    mime_type TEXT NOT NULL,
    duration_ms INTEGER NOT NULL,
    transcript TEXT,
    word_segments JSONB,
    pause_analysis JSONB,
    feedback JSONB,
    upload_status_code CHAR(7) NOT NULL DEFAULT 'UPL0001',
    feedback_status_code CHAR(7) NOT NULL DEFAULT 'FBS0001',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE refresh_token (
   id          BIGSERIAL PRIMARY KEY,
   user_id     BIGINT       NOT NULL,
   token       VARCHAR(512) NOT NULL,
   expires_at  TIMESTAMP    NOT NULL,
   created_at  TIMESTAMP    NOT NULL,

   CONSTRAINT uk_refresh_token UNIQUE (token)
);

CREATE TABLE feature_flags (
    flag_name   VARCHAR(100) PRIMARY KEY,
    enabled     BOOLEAN      NOT NULL DEFAULT true,
    description TEXT,
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 초기 데이터
INSERT INTO feature_flags (flag_name, enabled, description)
VALUES ('ai-for-free', true, '이벤트 또는 특별 기간동안 AI 기능 임시 허용');

CREATE TABLE drill_answers (
   id BIGSERIAL PRIMARY KEY,
   user_id BIGINT NOT NULL,
   question_id BIGINT NOT NULL,
   audio_url TEXT NOT NULL,
   storage_type VARCHAR(10) NOT NULL DEFAULT 'S3',
   mime_type TEXT NOT NULL,
   duration_ms INTEGER NOT NULL DEFAULT 0,
   transcript TEXT,
   word_segments JSONB,
   pause_analysis JSONB,
   feedback JSONB,
   upload_status_code CHAR(7) NOT NULL DEFAULT 'UPL0001',
   feedback_status_code CHAR(7) NOT NULL DEFAULT 'FBS0001',
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP
);