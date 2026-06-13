-- 소셜 계정을 provider별로 분리 저장 (Google + Kakao 동시 연결 지원)
CREATE TABLE user_social_accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider    VARCHAR(20) NOT NULL,                    -- 'google', 'kakao', 'naver'
    provider_user_id VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_social_accounts_user_id ON user_social_accounts(user_id);

-- V16에서 추가된 social_provider/social_id 컬럼 데이터를 이관
INSERT INTO user_social_accounts (user_id, provider, provider_user_id)
SELECT id, social_provider, social_id
FROM users
WHERE social_provider IS NOT NULL AND social_id IS NOT NULL;

-- 이관 완료 후 기존 컬럼 제거
ALTER TABLE users
    DROP COLUMN IF EXISTS social_provider,
    DROP COLUMN IF EXISTS social_id;
