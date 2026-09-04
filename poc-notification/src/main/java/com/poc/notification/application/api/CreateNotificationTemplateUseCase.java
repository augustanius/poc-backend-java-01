package com.poc.notification.application.api;

import com.poc.notification.domain.model.NotificationTemplate;

public interface CreateNotificationTemplateUseCase {

    NotificationTemplate create(NotificationTemplate template);
}
