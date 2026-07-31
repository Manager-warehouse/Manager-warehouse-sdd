CREATE TABLE IF NOT EXISTS user_refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_user_refresh_tokens_user_id
    ON user_refresh_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_user_refresh_tokens_expires_at
    ON user_refresh_tokens(expires_at);

INSERT INTO user_refresh_tokens (user_id, token_hash, expires_at, created_at)
SELECT id, refresh_token_hash, refresh_token_expires_at, NOW()
FROM users
WHERE refresh_token_hash IS NOT NULL
  AND refresh_token_expires_at IS NOT NULL
ON CONFLICT (token_hash) DO NOTHING;
