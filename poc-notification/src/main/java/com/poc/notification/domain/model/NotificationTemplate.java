package com.poc.notification.domain.model;

import com.poc.common.domain.AggregateRoot;

import java.util.Objects;

public class NotificationTemplate extends AggregateRoot<String> {

    private String id;
    private String name;
    private String subject;
    private String body;
    private String channel;

    public NotificationTemplate() {}

    public NotificationTemplate(String id, String name, String subject, String body, String channel) {
        this.id = id;
        this.name = name;
        this.subject = subject;
        this.body = body;
        this.channel = channel;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationTemplate that = (NotificationTemplate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
