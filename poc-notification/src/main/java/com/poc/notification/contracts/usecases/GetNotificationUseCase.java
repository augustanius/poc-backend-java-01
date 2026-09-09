package com.poc.notification.contracts.usecases;

import com.poc.common.domain.PageResult;
import com.poc.notification.domain.model.Notification;

import java.util.Optional;

public interface GetNotificationUseCase {

    Optional<Notification> findById(String id);

    PageResult<Notification> findByUserId(String userId, int page, int size);

    PageResult<Notification> findAll(int page, int size);
}
