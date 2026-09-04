package com.poc.notification.application.api;

import com.poc.common.domain.PageResult;
import com.poc.notification.domain.model.NotificationTemplate;

import java.util.Optional;

public interface GetNotificationTemplateUseCase {

    Optional<NotificationTemplate> findById(String id);

    Optional<NotificationTemplate> findByName(String name);

    PageResult<NotificationTemplate> findAll(int page, int size);
}
