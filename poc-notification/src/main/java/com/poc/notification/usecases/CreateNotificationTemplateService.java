package com.poc.notification.usecases;

import com.poc.notification.contracts.usecases.CreateNotificationTemplateUseCase;
import com.poc.notification.domain.model.NotificationTemplate;
import com.poc.notification.contracts.repositories.NotificationTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
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
