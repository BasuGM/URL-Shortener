CREATE TABLE short_urls (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code         VARCHAR(16)   NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_short_urls_code UNIQUE (code)
);
