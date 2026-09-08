package com.poc.common.event;

import com.poc.common.domain.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EmailEvent extends DomainEvent {

    private final String notificationId;
    private final String userId;
    private final String email;
    private final String subject;
    private final String body;
    private final String channel;

    @JsonCreator
    public EmailEvent(@JsonProperty("notificationId") String notificationId,
                      @JsonProperty("userId") String userId,
                      @JsonProperty("email") String email,
                      @JsonProperty("subject") String subject,
                      @JsonProperty("body") String body,
                      @JsonProperty("channel") String channel) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.email = email;
        this.subject = subject;
        this.body = body;
        this.channel = channel;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getChannel() {
        return channel;
    }
}
