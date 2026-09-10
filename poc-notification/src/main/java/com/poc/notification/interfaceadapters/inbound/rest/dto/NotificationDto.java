package com.poc.notification.interfaceadapters.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class NotificationDto {

    public record SendNotificationRequest(
            @NotBlank(message = "Template ID is required") String templateId,
            @NotBlank(message = "Subject is required") String subject,
            @NotBlank(message = "Body is required") String body,
            @NotBlank(message = "Channel is required") String channel
    ) {}

    public record NotificationResponse(
            String id, String userId, String templateId,
            String subject, String body, String channel,
            String status, Instant createdAt, Instant sentAt) {}
}
