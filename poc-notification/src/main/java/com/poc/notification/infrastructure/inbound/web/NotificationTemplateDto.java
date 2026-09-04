package com.poc.notification.infrastructure.inbound.web;

public class NotificationTemplateDto {

    public record CreateTemplateRequest(String name, String subject, String body, String channel) {}

    public record TemplateResponse(String id, String name, String subject, String body, String channel) {}
}
