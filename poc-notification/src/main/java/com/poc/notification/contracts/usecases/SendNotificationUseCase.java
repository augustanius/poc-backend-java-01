package com.poc.notification.contracts.usecases;

import com.poc.notification.domain.model.Notification;

public interface SendNotificationUseCase {

    Notification send(String userId, String templateId, String subject, String body, String channel);
}
