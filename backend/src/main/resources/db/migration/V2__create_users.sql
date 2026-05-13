CREATE TABLE users
(
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    nickname            VARCHAR(50)  NOT NULL,
    profile_image_url   TEXT,
    bio                 TEXT,
    -- SHA-256 해시로 저장. 로그인 시 발급, 로그아웃 시 NULL, 재발급 시 비교 후 rotate
    refresh_token_hash  VARCHAR(512),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,

    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_users_email    ON users (email);
CREATE UNIQUE INDEX uk_users_nickname ON users (nickname);
CREATE INDEX idx_users_deleted_at     ON users (deleted_at);
