package com.poc.notification.infrastructure.inbound.web;

import java.time.Instant;

public class NotificationDto {

    public record SendNotificationRequest(String templateId, String subject, String body, String channel) {}

    public record NotificationResponse(
            String id, String userId, String templateId,
            String subject, String body, String channel,
            String status, Instant createdAt, Instant sentAt) {}
}
