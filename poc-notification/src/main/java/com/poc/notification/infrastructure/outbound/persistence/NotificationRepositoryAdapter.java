package com.poc.notification.infrastructure.outbound.persistence;

import com.poc.common.domain.PageResult;
import com.poc.notification.domain.model.Notification;
import com.poc.notification.domain.spi.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = toEntity(notification);
        NotificationJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public PageResult<Notification> findByUserId(String userId, int page, int size) {
        var result = jpaRepository.findByUserId(userId, PageRequest.of(page, size));
        return toPageResult(result);
    }

    @Override
    public PageResult<Notification> findAll(int page, int size) {
        var result = jpaRepository.findAll(PageRequest.of(page, size));
        return toPageResult(result);
    }

    private PageResult<Notification> toPageResult(org.springframework.data.domain.Page<NotificationJpaEntity> page) {
        return PageResult.of(
                page.getContent().stream().map(this::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private Notification toDomain(NotificationJpaEntity entity) {
        Notification n = new Notification(entity.getId(), entity.getUserId(), entity.getTemplateId(),
                entity.getSubject(), entity.getBody(), entity.getChannel());
        n.setStatus(Notification.Status.valueOf(entity.getStatus().name()));
        n.setCreatedAt(entity.getCreatedAt());
        n.setSentAt(entity.getSentAt());
        return n;
    }

    private NotificationJpaEntity toEntity(Notification notification) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(notification.getId());
        entity.setUserId(notification.getUserId());
        entity.setTemplateId(notification.getTemplateId());
        entity.setSubject(notification.getSubject());
        entity.setBody(notification.getBody());
        entity.setChannel(notification.getChannel());
        entity.setStatus(NotificationJpaEntity.NotificationStatus.valueOf(notification.getStatus().name()));
        entity.setCreatedAt(notification.getCreatedAt());
        entity.setSentAt(notification.getSentAt());
        return entity;
    }
}
