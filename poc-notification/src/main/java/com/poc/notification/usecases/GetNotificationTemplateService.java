package com.poc.notification.usecases;

import com.poc.common.domain.PageResult;
import com.poc.notification.contracts.usecases.GetNotificationTemplateUseCase;
import com.poc.notification.domain.model.NotificationTemplate;
import com.poc.notification.contracts.repositories.NotificationTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetNotificationTemplateService implements GetNotificationTemplateUseCase {

    private final NotificationTemplateRepository templateRepository;

    public GetNotificationTemplateService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public Optional<NotificationTemplate> findById(String id) {
        return templateRepository.findById(id);
    }

    @Override
    public Optional<NotificationTemplate> findByName(String name) {
        return templateRepository.findByName(name);
    }

    @Override
    public PageResult<NotificationTemplate> findAll(int page, int size) {
        return templateRepository.findAll(page, size);
    }
}
