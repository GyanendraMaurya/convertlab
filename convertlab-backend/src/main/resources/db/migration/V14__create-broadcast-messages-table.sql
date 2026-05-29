CREATE TABLE broadcast_messages
(
    id         UUID PRIMARY KEY,
    message    VARCHAR(500) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_broadcast_messages_active_expires_at
    ON broadcast_messages (active, expires_at DESC);

CREATE INDEX idx_broadcast_messages_created_at
    ON broadcast_messages (created_at DESC);
