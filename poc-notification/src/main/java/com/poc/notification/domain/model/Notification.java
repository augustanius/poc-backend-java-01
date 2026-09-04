package com.poc.notification.domain.model;

import com.poc.common.domain.AggregateRoot;

import java.time.Instant;
import java.util.Objects;

public class Notification extends AggregateRoot<String> {

    public enum Status { PENDING, SENT, FAILED }

    private String id;
    private String userId;
    private String templateId;
    private String subject;
    private String body;
    private String channel;
    private Status status;
    private Instant createdAt;
    private Instant sentAt;

    public Notification() {
        this.createdAt = Instant.now();
        this.status = Status.PENDING;
    }

    public Notification(String id, String userId, String templateId, String subject, String body, String channel) {
        this.id = id;
        this.userId = userId;
        this.templateId = templateId;
        this.subject = subject;
        this.body = body;
        this.channel = channel;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
