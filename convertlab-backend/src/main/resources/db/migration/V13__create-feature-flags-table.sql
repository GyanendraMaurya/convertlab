CREATE TABLE feature_flags
(
    code               VARCHAR(100) PRIMARY KEY,
    title              VARCHAR(200) NOT NULL,
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    expose_to_frontend BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO feature_flags (code, title, enabled, expose_to_frontend)
VALUES ('SHOW_CONTACT_PAGE', 'Show contact page', TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;
