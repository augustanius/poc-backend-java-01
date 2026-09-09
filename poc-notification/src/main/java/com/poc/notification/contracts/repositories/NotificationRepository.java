package com.poc.notification.contracts.repositories;

import com.poc.common.domain.PageResult;
import com.poc.notification.domain.model.Notification;

import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(String id);

    PageResult<Notification> findByUserId(String userId, int page, int size);

    PageResult<Notification> findAll(int page, int size);
}
