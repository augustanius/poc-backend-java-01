package com.poc.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public OutboxPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String aggregateType, String aggregateId,
                        String eventType, Object eventPayload, UUID sagaId) {
        try {
            String payload = objectMapper.writeValueAsString(eventPayload);
            OutboxEvent event = new OutboxEvent(
                    UUID.randomUUID(),
                    aggregateType,
                    aggregateId,
                    eventType,
                    payload,
                    sagaId,
                    Instant.now()
            );
            outboxEventRepository.save(event);
            log.debug("Published outbox event: type={}, aggregate={}", eventType, aggregateId);
        } catch (Exception e) {
            log.error("Failed to publish outbox event: type={}, aggregate={}", eventType, aggregateId, e);
            throw new RuntimeException("Failed to publish outbox event", e);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String aggregateType, String aggregateId,
                        String eventType, Object eventPayload) {
        publish(aggregateType, aggregateId, eventType, eventPayload, null);
    }
}
