-- RatioLab schema, version 2: financial facts as reported to the SEC.
-- Every reported value is kept, including restated ones; which one is shown is decided when reading.

CREATE TABLE financial_fact (
    company_id    BIGINT       NOT NULL REFERENCES company (id),
    concept       VARCHAR(200) NOT NULL,   -- e.g. us-gaap:NetIncomeLoss
    unit          VARCHAR(20)  NOT NULL,   -- e.g. USD, USD/shares
    period_start  DATE         NOT NULL,
    period_end    DATE         NOT NULL,
    accession     VARCHAR(20)  NOT NULL,   -- the filing that reported the value
    value         NUMERIC      NOT NULL,   -- no fixed precision, so nothing is rounded
    form          VARCHAR(20)  NOT NULL,   -- e.g. 10-K, 10-K/A, 10-Q
    filed_at      DATE         NOT NULL,
    import_run_id BIGINT       NOT NULL REFERENCES import_run (id),
    retrieved_at  TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (company_id, concept, unit, period_start, period_end, accession),
    CHECK (period_start <= period_end)
);
