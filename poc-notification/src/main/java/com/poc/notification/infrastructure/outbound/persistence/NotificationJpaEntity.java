package com.poc.notification.infrastructure.outbound.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications", schema = "poc_notification")
public class NotificationJpaEntity {

    @Id
    private String id;

    private String userId;

    private String templateId;

    private String subject;

    private String body;

    private String channel;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private Instant createdAt;

    private Instant sentAt;

    public NotificationJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public enum NotificationStatus { PENDING, SENT, FAILED }
}
