-- OPIc 연습 앱 Database Schema (PostgreSQL)
-- 생성일: 2026-02-02
-- 버전: 1.0.0 (MVP)

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    email TEXT UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE question_type (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE questions (
    id SERIAL PRIMARY KEY,
    category_id INTEGER NOT NULL,
    question_type_id INTEGER NOT NULL,
    question TEXT NOT NULL,
    audio_file_url TEXT,
    duration_ms INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE question_sets (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE TABLE question_set_items (
    item_id SERIAL PRIMARY KEY,
    question_set_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
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
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    question_set_id INTEGER,
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
    id SERIAL PRIMARY KEY,
    question_id INTEGER NOT NULL,
    session_id INTEGER NOT NULL,
    audio_url TEXT NOT NULL,
    storage_type VARCHAR(10) NOT NULL,
    mime_type TEXT NOT NULL,
    duration_ms INTEGER NOT NULL,
    transcript TEXT,
    upload_status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);
