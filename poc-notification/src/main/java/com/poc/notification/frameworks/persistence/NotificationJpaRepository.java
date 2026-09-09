package com.poc.notification.frameworks.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, String> {

    Page<NotificationJpaEntity> findByUserId(String userId, Pageable pageable);
}
