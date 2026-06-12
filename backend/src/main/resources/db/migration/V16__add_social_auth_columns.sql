-- 소셜 로그인 지원을 위한 컬럼 추가
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS social_provider VARCHAR(20) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS social_id       VARCHAR(255) DEFAULT NULL;

-- 소셜 provider + id 조합은 유일해야 함
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_social
    ON users (social_provider, social_id)
    WHERE social_provider IS NOT NULL AND social_id IS NOT NULL;

-- 소셜 유저는 password_hash가 없을 수 있으므로 nullable 허용
ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;
