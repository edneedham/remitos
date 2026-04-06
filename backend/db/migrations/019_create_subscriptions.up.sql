-- Migration 019: Create subscriptions table for billing

CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'trialing',
    device_connected BOOLEAN NOT NULL DEFAULT FALSE,
    features JSONB NOT NULL DEFAULT '{"offlineMode": true, "connectedMode": true, "premiumFeatures": true}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_device_id ON subscriptions(device_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);
