package com.poc.notification.publicapi;

import com.poc.common.domain.PageResult;

import java.time.Instant;
import java.util.Optional;

public interface NotificationPublicApi {

    boolean notificationExists(String notificationId);

    Optional<NotificationInfo> findById(String notificationId);

    PageResult<NotificationInfo> findByUserId(String userId, int page, int size);

    record NotificationInfo(String id, String userId, String subject, String channel, String status, Instant createdAt) {}
}
