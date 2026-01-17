CREATE TABLE page_visit
(
    id         UUID PRIMARY KEY,
    session_id UUID         NOT NULL,
    path       VARCHAR(255) NOT NULL,
    entry      BOOLEAN,
    visited_at TIMESTAMP    NOT NULL
);

