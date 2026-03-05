CREATE TABLE user_ai_usage
(
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    usage_date      DATE         NOT NULL DEFAULT CURRENT_DATE,
    ingest_count    INTEGER      NOT NULL DEFAULT 0,
    query_count     INTEGER      NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_ai_usage_email_date
        UNIQUE (email, usage_date)
);