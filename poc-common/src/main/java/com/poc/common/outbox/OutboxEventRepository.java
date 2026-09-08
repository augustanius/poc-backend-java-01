package com.poc.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Modifying
    @Query(value = "SELECT * FROM poc_outbox.outbox_events " +
            "WHERE published = FALSE " +
            "ORDER BY created_at " +
            "LIMIT :batchSize " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> claimEvents(@Param("batchSize") int batchSize);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.published = true AND e.publishedAt < :cutoff")
    int deleteByPublishedTrueAndPublishedAtBefore(@Param("cutoff") Instant cutoff);
}
