ALTER TABLE users
    ADD COLUMN credential_version INTEGER NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uk_users_email_lower_active
    ON users (LOWER(email))
    WHERE deleted_at IS NULL;

CREATE TABLE auth_verification_challenges (
    id                      UUID PRIMARY KEY,
    email                   VARCHAR(255) NOT NULL,
    purpose                 VARCHAR(30) NOT NULL,
    code_hash               VARCHAR(255) NOT NULL,
    verification_token_hash VARCHAR(64),
    failed_attempts         INTEGER NOT NULL DEFAULT 0,
    expires_at              TIMESTAMPTZ NOT NULL,
    verified_at             TIMESTAMPTZ,
    consumed_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_auth_verification_purpose
        CHECK (purpose IN ('SIGNUP', 'PASSWORD_RESET'))
);

CREATE INDEX idx_auth_verification_lookup
    ON auth_verification_challenges (email, purpose, created_at DESC);

CREATE UNIQUE INDEX uk_auth_verification_token
    ON auth_verification_challenges (verification_token_hash)
    WHERE verification_token_hash IS NOT NULL;
