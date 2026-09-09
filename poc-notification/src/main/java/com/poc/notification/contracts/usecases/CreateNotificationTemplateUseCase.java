package com.poc.notification.contracts.usecases;

import com.poc.notification.domain.model.NotificationTemplate;

public interface CreateNotificationTemplateUseCase {

    NotificationTemplate create(NotificationTemplate template);
}
