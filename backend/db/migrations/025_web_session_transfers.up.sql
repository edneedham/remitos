CREATE TABLE IF NOT EXISTS web_session_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    desktop_refresh_token_id UUID NOT NULL REFERENCES refresh_tokens(id),
    phone_refresh_token_id UUID REFERENCES refresh_tokens(id),
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_web_session_transfers_user_id ON web_session_transfers(user_id);
CREATE INDEX IF NOT EXISTS idx_web_session_transfers_expires_at ON web_session_transfers(expires_at);
CREATE INDEX IF NOT EXISTS idx_web_session_transfers_used_at ON web_session_transfers(used_at);
