package com.poc.notification.frameworks.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateJpaRepository extends JpaRepository<NotificationTemplateJpaEntity, String> {

    Optional<NotificationTemplateJpaEntity> findByName(String name);
}
