package com.poc.notification.application.service;

import com.poc.common.domain.PageResult;
import com.poc.notification.application.api.GetNotificationUseCase;
import com.poc.notification.domain.model.Notification;
import com.poc.notification.domain.spi.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetNotificationService implements GetNotificationUseCase {

    private final NotificationRepository notificationRepository;

    public GetNotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Optional<Notification> findById(String id) {
        return notificationRepository.findById(id);
    }

    @Override
    public PageResult<Notification> findByUserId(String userId, int page, int size) {
        return notificationRepository.findByUserId(userId, page, size);
    }

    @Override
    public PageResult<Notification> findAll(int page, int size) {
        return notificationRepository.findAll(page, size);
    }
}
