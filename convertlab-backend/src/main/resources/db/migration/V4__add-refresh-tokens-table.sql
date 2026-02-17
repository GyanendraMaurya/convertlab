CREATE TABLE refresh_tokens
(
    id         UUID PRIMARY KEY,            -- matches JWT "jti" claim
    email      VARCHAR(255) NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);