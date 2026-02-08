ALTER TABLE page_visit
    ADD COLUMN ip_hash VARCHAR(64),
    ADD COLUMN city VARCHAR(100),
    ADD COLUMN country VARCHAR(100),
    ADD COLUMN country_code VARCHAR(2);