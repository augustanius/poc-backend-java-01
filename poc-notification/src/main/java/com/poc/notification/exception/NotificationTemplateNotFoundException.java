package com.poc.notification.exception;

import com.poc.common.exception.NotFoundException;

import java.util.Map;

public class NotificationTemplateNotFoundException extends NotFoundException {

    public NotificationTemplateNotFoundException(String id) {
        super("Notification template not found: " + id, Map.of("templateId", id));
    }
}
