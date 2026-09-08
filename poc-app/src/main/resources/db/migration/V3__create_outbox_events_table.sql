CREATE SCHEMA IF NOT EXISTS poc_outbox;

CREATE TABLE IF NOT EXISTS poc_outbox.outbox_events (
    id              UUID            PRIMARY KEY,
    aggregate_type  VARCHAR(100)    NOT NULL,
    aggregate_id    VARCHAR(100)    NOT NULL,
    event_type      VARCHAR(200)    NOT NULL,
    payload         JSONB           NOT NULL,
    saga_id         UUID            NULL,
    retry_count     INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    published       BOOLEAN         NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMP       NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON poc_outbox.outbox_events (created_at)
    WHERE published = FALSE;

CREATE INDEX IF NOT EXISTS idx_outbox_published_old
    ON poc_outbox.outbox_events (published_at)
    WHERE published = TRUE;
