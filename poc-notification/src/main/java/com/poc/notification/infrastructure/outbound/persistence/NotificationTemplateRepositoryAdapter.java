package com.poc.notification.infrastructure.outbound.persistence;

import com.poc.common.domain.PageResult;
import com.poc.notification.domain.model.NotificationTemplate;
import com.poc.notification.domain.spi.NotificationTemplateRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NotificationTemplateRepositoryAdapter implements NotificationTemplateRepository {

    private final NotificationTemplateJpaRepository jpaRepository;

    public NotificationTemplateRepositoryAdapter(NotificationTemplateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public NotificationTemplate save(NotificationTemplate template) {
        NotificationTemplateJpaEntity entity = toEntity(template);
        NotificationTemplateJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<NotificationTemplate> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<NotificationTemplate> findByName(String name) {
        return jpaRepository.findByName(name).map(this::toDomain);
    }

    @Override
    public PageResult<NotificationTemplate> findAll(int page, int size) {
        var result = jpaRepository.findAll(PageRequest.of(page, size));
        return PageResult.of(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    private NotificationTemplate toDomain(NotificationTemplateJpaEntity entity) {
        return new NotificationTemplate(entity.getId(), entity.getName(),
                entity.getSubject(), entity.getBody(), entity.getChannel());
    }

    private NotificationTemplateJpaEntity toEntity(NotificationTemplate template) {
        NotificationTemplateJpaEntity entity = new NotificationTemplateJpaEntity();
        entity.setId(template.getId());
        entity.setName(template.getName());
        entity.setSubject(template.getSubject());
        entity.setBody(template.getBody());
        entity.setChannel(template.getChannel());
        return entity;
    }
}
