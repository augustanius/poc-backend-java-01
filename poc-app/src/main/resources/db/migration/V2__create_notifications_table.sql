CREATE SCHEMA IF NOT EXISTS poc_notification;

CREATE TABLE IF NOT EXISTS poc_notification.notifications (
    id              VARCHAR(36)    NOT NULL,
    user_id         VARCHAR(36)    NOT NULL,
    template_id     VARCHAR(36),
    subject         VARCHAR(500)   NOT NULL,
    body            TEXT           NOT NULL,
    channel         VARCHAR(50)    NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP      NOT NULL,
    sent_at         TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS poc_notification.notification_templates (
    id              VARCHAR(36)    NOT NULL,
    name            VARCHAR(200)   NOT NULL,
    subject         VARCHAR(500)   NOT NULL,
    body            TEXT           NOT NULL,
    channel         VARCHAR(50)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_template_name UNIQUE (name)
);
