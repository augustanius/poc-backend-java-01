package com.poc.notification.application.service;

import com.poc.notification.application.api.SendNotificationUseCase;
import com.poc.notification.domain.model.Notification;
import com.poc.notification.domain.spi.NotificationRepository;
import com.poc.user.publicapi.UserPublicApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SendNotificationService implements SendNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserPublicApi userPublicApi;

    public SendNotificationService(NotificationRepository notificationRepository,
                                   UserPublicApi userPublicApi) {
        this.notificationRepository = notificationRepository;
        this.userPublicApi = userPublicApi;
    }

    @Override
    @Transactional
    public Notification send(String userId, String templateId, String subject, String body, String channel) {
        var userInfo = userPublicApi.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        log.info("Sending notification to user {} ({}) via {}", userInfo.username(), userInfo.email(), channel);

        Notification notification = new Notification(
                UUID.randomUUID().toString(), userId, templateId, subject, body, channel);

        // In a real app, you would call an email/SMS/push provider here
        notification.setStatus(Notification.Status.SENT);
        notification.setSentAt(Instant.now());

        return notificationRepository.save(notification);
    }
}
