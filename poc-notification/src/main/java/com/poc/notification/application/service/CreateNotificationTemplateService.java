package com.poc.notification.application.service;

import com.poc.notification.application.api.CreateNotificationTemplateUseCase;
import com.poc.notification.domain.model.NotificationTemplate;
import com.poc.notification.domain.spi.NotificationTemplateRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateNotificationTemplateService implements CreateNotificationTemplateUseCase {

    private final NotificationTemplateRepository templateRepository;

    public CreateNotificationTemplateService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public NotificationTemplate create(NotificationTemplate template) {
        return templateRepository.save(template);
    }
}
