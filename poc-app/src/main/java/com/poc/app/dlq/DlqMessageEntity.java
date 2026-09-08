package com.poc.app.dlq;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "dlq_messages", schema = "poc_dlq")
public class DlqMessageEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "original_topic", length = 200)
    private String originalTopic;

    @Column(name = "event_key", length = 100)
    private String eventKey;

    @Column(name = "event_type", length = 200)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DlqMessageEntity() {
    }

    public DlqMessageEntity(String id, String topic, String originalTopic, String eventKey,
                            String eventType, String payload, String errorMessage,
                            int retryCount, Instant receivedAt, Instant failedAt) {
        this.id = id;
        this.topic = topic;
        this.originalTopic = originalTopic;
        this.eventKey = eventKey;
        this.eventType = eventType;
        this.payload = payload;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.receivedAt = receivedAt;
        this.failedAt = failedAt;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getOriginalTopic() {
        return originalTopic;
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
