ALTER TABLE users
DROP COLUMN IF EXISTS password_hash;

ALTER TABLE users
ADD CONSTRAINT uq_users_email UNIQUE (email);

-- Create auth_providers table
CREATE TABLE auth_providers (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL, -- 'local', 'google', etc.

    provider_user_id VARCHAR(255), -- Google sub, GitHub id
    password_hash VARCHAR(255),    -- only for local auth

    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),

    CONSTRAINT fk_auth_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Prevent duplicate OAuth identities
CREATE UNIQUE INDEX uq_provider_user
ON auth_providers(provider, provider_user_id)
WHERE provider_user_id IS NOT NULL;

-- Prevent multiple local auth entries per user
CREATE UNIQUE INDEX uq_local_user
ON auth_providers(user_id, provider)
WHERE provider = 'local';