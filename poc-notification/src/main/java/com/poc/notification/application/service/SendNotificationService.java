package com.poc.notification.application.service;

import com.poc.common.event.EmailEvent;
import com.poc.common.outbox.OutboxPublisher;
import com.poc.notification.application.api.SendNotificationUseCase;
import com.poc.notification.domain.model.Notification;
import com.poc.notification.domain.spi.NotificationRepository;
import com.poc.user.publicapi.UserPublicApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SendNotificationService implements SendNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserPublicApi userPublicApi;
    private final OutboxPublisher outboxPublisher;

    public SendNotificationService(NotificationRepository notificationRepository,
                                   UserPublicApi userPublicApi,
                                   OutboxPublisher outboxPublisher) {
        this.notificationRepository = notificationRepository;
        this.userPublicApi = userPublicApi;
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    @Transactional
    public Notification send(String userId, String templateId, String subject, String body, String channel) {
        var userInfo = userPublicApi.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        log.info("Queuing notification for user {} ({}) via {}", userInfo.username(), userInfo.email(), channel);

        Notification notification = new Notification(
                UUID.randomUUID().toString(), userId, templateId, subject, body, channel);

        notification = notificationRepository.save(notification);

        EmailEvent emailEvent = new EmailEvent(
                notification.getId(),
                userId,
                userInfo.email(),
                subject,
                body,
                channel
        );

        outboxPublisher.publish(
                "Notification",
                notification.getId(),
                "EmailNotificationRequested",
                emailEvent
        );

        return notification;
    }
}
