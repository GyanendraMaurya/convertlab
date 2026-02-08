CREATE TABLE users
(
    id              UUID PRIMARY KEY,

    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,

    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX uk_users_email ON users(email);

CREATE TABLE email_otp
(
    id           UUID PRIMARY KEY,

    email        VARCHAR(255) NOT NULL,
    otp_hash     VARCHAR(255) NOT NULL,

    expires_at   TIMESTAMPTZ  NOT NULL,
    consumed     BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at   TIMESTAMPTZ  NOT NULL
);
