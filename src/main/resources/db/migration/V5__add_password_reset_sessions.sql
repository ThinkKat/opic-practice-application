CREATE TABLE password_reset_session (
    id                UUID PRIMARY KEY,
    user_id           BIGINT,
    code_hash         VARCHAR(256),
    expires_at        TIMESTAMP,
    used_at           TIMESTAMP,
    attempt_count     INT          NOT NULL DEFAULT 0,
    resend_count      INT          NOT NULL DEFAULT 0,
    last_resent_at    TIMESTAMP,
    blocked_until     TIMESTAMP,
    verified_at       TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL
);

