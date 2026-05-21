-- PrediX Oracle Ops schema (UTC timestamps)

CREATE TABLE oracle_sources (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL UNIQUE,
    type            VARCHAR(32)  NOT NULL DEFAULT 'HTTP_API',
    base_url        VARCHAR(512) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    priority        INT          NOT NULL DEFAULT 100,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE TABLE oracle_evidences (
    id                      BIGSERIAL PRIMARY KEY,
    market_id               VARCHAR(64)  NOT NULL,
    source_id               BIGINT       NOT NULL REFERENCES oracle_sources(id),
    source_url              VARCHAR(1024) NOT NULL,
    fetched_at              TIMESTAMPTZ  NOT NULL,
    raw_payload             JSONB        NOT NULL,
    normalized_outcome_code VARCHAR(64)  NOT NULL,
    confidence_score        NUMERIC(10, 6),
    hash_digest             VARCHAR(128) NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_oracle_evidences_market ON oracle_evidences(market_id);
CREATE INDEX idx_oracle_evidences_market_fetched ON oracle_evidences(market_id, fetched_at DESC);

CREATE TABLE resolution_jobs (
    id               BIGSERIAL PRIMARY KEY,
    job_code         VARCHAR(64)  NOT NULL UNIQUE,
    market_id        VARCHAR(64)  NOT NULL,
    job_type         VARCHAR(32)  NOT NULL,
    status           VARCHAR(32)  NOT NULL,
    idempotency_key  VARCHAR(256) NOT NULL UNIQUE,
    retry_count      INT          NOT NULL DEFAULT 0,
    next_retry_at    TIMESTAMPTZ,
    payload          JSONB,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_resolution_jobs_market ON resolution_jobs(market_id);
CREATE INDEX idx_resolution_jobs_status ON resolution_jobs(status);
CREATE INDEX idx_resolution_jobs_next_retry ON resolution_jobs(next_retry_at) WHERE status IN ('FAILED', 'RETRYING');

CREATE TABLE uma_transactions (
    id            BIGSERIAL PRIMARY KEY,
    market_id     VARCHAR(64)  NOT NULL,
    action_type   VARCHAR(32)  NOT NULL,
    chain_id      BIGINT       NOT NULL,
    tx_hash       VARCHAR(128),
    assertion_id  VARCHAR(128),
    request_id    VARCHAR(128),
    tx_status     VARCHAR(32)  NOT NULL,
    submitted_at  TIMESTAMPTZ,
    confirmed_at  TIMESTAMPTZ,
    raw_receipt   JSONB,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_uma_transactions_market ON uma_transactions(market_id);
CREATE INDEX idx_uma_transactions_assertion ON uma_transactions(assertion_id);

CREATE TABLE resolution_audits (
    id              BIGSERIAL PRIMARY KEY,
    market_id       VARCHAR(64)  NOT NULL,
    phase           VARCHAR(64)  NOT NULL,
    actor_type      VARCHAR(16)  NOT NULL,
    action          VARCHAR(128) NOT NULL,
    input_snapshot  JSONB,
    output_snapshot JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_resolution_audits_market ON resolution_audits(market_id);
