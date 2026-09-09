package com.poc.app.usecases;

import com.poc.common.event.EmailEvent;
import com.poc.common.event.UserRegisteredEvent;
import com.poc.common.contracts.outbox.OutboxPublisher;
import com.poc.notification.domain.model.Notification;
import com.poc.notification.contracts.repositories.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class UserRegisteredEmailListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEmailListener.class);

    private final NotificationRepository notificationRepository;
    private final OutboxPublisher outboxPublisher;

    public UserRegisteredEmailListener(NotificationRepository notificationRepository,
                                       OutboxPublisher outboxPublisher) {
        this.notificationRepository = notificationRepository;
        this.outboxPublisher = outboxPublisher;
    }

    @EventListener
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("User registered: {} ({}), sending welcome email", event.getUsername(), event.getEmail());

        String subject = "Welcome to PoC!";
        String body = "<h1>Welcome, " + event.getUsername() + "!</h1>"
                + "<p>Thank you for registering. Your account is now active.</p>";

        Notification notification = new Notification(
                UUID.randomUUID().toString(),
                event.getUserId(),
                null,
                subject,
                body,
                "email"
        );

        notification = notificationRepository.save(notification);

        EmailEvent emailEvent = new EmailEvent(
                notification.getId(),
                event.getUserId(),
                event.getEmail(),
                subject,
                body,
                "email"
        );

        outboxPublisher.publish(
                "Notification",
                notification.getId(),
                "EmailNotificationRequested",
                emailEvent
        );

        log.info("Welcome email queued for user {}", event.getUsername());
    }
}
