-- In-app notifications for web panel users (per-user, company-scoped for tenancy checks)
CREATE TABLE user_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    kind TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT,
    action_url TEXT,
    read_at TIMESTAMPTZ,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_notifications_user_created
    ON user_notifications (user_id, created_at DESC);

CREATE INDEX idx_user_notifications_company_created
    ON user_notifications (company_id, created_at DESC);

CREATE INDEX idx_user_notifications_unread
    ON user_notifications (user_id)
    WHERE read_at IS NULL;
