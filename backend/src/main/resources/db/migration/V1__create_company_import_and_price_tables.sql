-- RatioLab schema, version 1: companies, import runs and daily prices.
-- Monetary values are exact decimals (numeric), never floating point.

CREATE TABLE company (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticker VARCHAR(10) NOT NULL UNIQUE,
    cik    VARCHAR(10) UNIQUE,        -- filled by the SEC import (M2)
    name   TEXT                       -- filled by the SEC import (M2)
);

-- One row per started import, successful or not.
CREATE TABLE import_run (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider         VARCHAR(20)  NOT NULL,
    company_id       BIGINT       NOT NULL REFERENCES company (id),
    started_at       TIMESTAMPTZ  NOT NULL,
    finished_at      TIMESTAMPTZ,
    result           VARCHAR(10)  NOT NULL CHECK (result IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    error_code       VARCHAR(40),
    error_message    TEXT,
    request_count    INTEGER      NOT NULL DEFAULT 0,
    rows_inserted    INTEGER      NOT NULL DEFAULT 0,
    rows_skipped     INTEGER      NOT NULL DEFAULT 0,  -- days that were already stored
    last_refreshed   DATE,                 -- last trading day reported by the provider
    payload_sha256   VARCHAR(64)           -- checksum of the archived raw response
);

CREATE INDEX import_run_provider_started_idx ON import_run (provider, started_at);

-- Daily prices as delivered (unadjusted). A stored row is never changed by a later import.
CREATE TABLE daily_price (
    company_id       BIGINT        NOT NULL REFERENCES company (id),
    trade_date       DATE          NOT NULL,
    provider         VARCHAR(20)   NOT NULL,
    adjustment       VARCHAR(12)   NOT NULL,
    open             NUMERIC(19, 4) NOT NULL,
    high             NUMERIC(19, 4) NOT NULL,
    low              NUMERIC(19, 4) NOT NULL,
    close            NUMERIC(19, 4) NOT NULL,
    volume           BIGINT        NOT NULL CHECK (volume >= 0),
    source_time_zone VARCHAR(40)   NOT NULL,   -- time zone of trade_date as stated by the provider
    import_run_id    BIGINT        NOT NULL REFERENCES import_run (id),
    retrieved_at     TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (company_id, trade_date, provider, adjustment),
    CHECK (low <= LEAST(open, close) AND high >= GREATEST(open, close))
);
