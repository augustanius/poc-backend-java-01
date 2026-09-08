package com.poc.common.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", schema = "poc_outbox")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "saga_id")
    private UUID sagaId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published", nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(UUID id, String aggregateType, String aggregateId,
                       String eventType, String payload, UUID sagaId, Instant createdAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.sagaId = sagaId;
        this.createdAt = createdAt;
        this.retryCount = 0;
        this.published = false;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isPublished() {
        return published;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
