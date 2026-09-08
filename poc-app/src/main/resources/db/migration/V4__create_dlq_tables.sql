CREATE SCHEMA IF NOT EXISTS poc_dlq;

CREATE TABLE poc_dlq.dlq_messages (
    id              VARCHAR(36)     PRIMARY KEY,
    topic           VARCHAR(200)    NOT NULL,
    event_key       VARCHAR(100),
    event_type      VARCHAR(200),
    payload         JSONB           NOT NULL,
    error_message   TEXT,
    retry_count     INT             NOT NULL DEFAULT 0,
    received_at     TIMESTAMP       NOT NULL,
    failed_at       TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dlq_messages_topic ON poc_dlq.dlq_messages(topic);
CREATE INDEX idx_dlq_messages_received ON poc_dlq.dlq_messages(received_at);
CREATE INDEX idx_dlq_messages_created ON poc_dlq.dlq_messages(created_at);
