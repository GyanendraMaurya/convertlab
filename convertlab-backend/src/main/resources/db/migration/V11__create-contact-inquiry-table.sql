CREATE TABLE contact_inquiry
(
    id                        UUID PRIMARY KEY,
    full_name                 VARCHAR(120) NOT NULL,
    email                     VARCHAR(255),
    phone                     VARCHAR(40),
    message                   TEXT         NOT NULL,
    inquiry_type              VARCHAR(60),
    budget_range              VARCHAR(60),
    status                    VARCHAR(30)  NOT NULL DEFAULT 'NEW',
    email_notification_status VARCHAR(30)  NOT NULL,
    email_notification_error  TEXT,
    ip_hash                   VARCHAR(64),
    user_agent                VARCHAR(512),
    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_contact_inquiry_created_at ON contact_inquiry(created_at);
CREATE INDEX idx_contact_inquiry_status ON contact_inquiry(status);
