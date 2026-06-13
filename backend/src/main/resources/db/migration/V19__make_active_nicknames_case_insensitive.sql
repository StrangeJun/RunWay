DROP INDEX IF EXISTS uk_users_nickname;

CREATE UNIQUE INDEX uk_users_nickname_lower_active
    ON users (LOWER(BTRIM(nickname)))
    WHERE deleted_at IS NULL;
