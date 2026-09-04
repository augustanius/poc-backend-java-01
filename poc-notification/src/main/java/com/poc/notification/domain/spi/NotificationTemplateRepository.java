package com.poc.notification.domain.spi;

import com.poc.common.domain.PageResult;
import com.poc.notification.domain.model.NotificationTemplate;

import java.util.Optional;

public interface NotificationTemplateRepository {

    NotificationTemplate save(NotificationTemplate template);

    Optional<NotificationTemplate> findById(String id);

    Optional<NotificationTemplate> findByName(String name);

    PageResult<NotificationTemplate> findAll(int page, int size);

    void deleteById(String id);
}
