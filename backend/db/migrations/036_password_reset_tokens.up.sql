-- Single-use password reset tokens (raw token is sent by email; only SHA-256 hash is stored).
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_tokens_token_hash
    ON password_reset_tokens (token_hash)
    WHERE used_at IS NULL;

CREATE INDEX idx_password_reset_tokens_user_active
    ON password_reset_tokens (user_id)
    WHERE used_at IS NULL;
