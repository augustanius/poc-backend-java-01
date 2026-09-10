package com.poc.notification.exception;

import com.poc.common.exception.NotFoundException;

import java.util.Map;

public class NotificationNotFoundException extends NotFoundException {

    public NotificationNotFoundException(String id) {
        super("Notification not found: " + id, Map.of("notificationId", id));
    }
}
