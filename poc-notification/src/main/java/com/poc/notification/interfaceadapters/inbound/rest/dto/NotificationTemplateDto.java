package com.poc.notification.interfaceadapters.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;

public class NotificationTemplateDto {

    public record CreateTemplateRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Subject is required") String subject,
            @NotBlank(message = "Body is required") String body,
            @NotBlank(message = "Channel is required") String channel
    ) {}

    public record TemplateResponse(String id, String name, String subject, String body, String channel) {}
}
