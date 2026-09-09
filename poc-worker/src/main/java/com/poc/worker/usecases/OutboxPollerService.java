package com.poc.worker.usecases;

import com.poc.common.frameworks.outbox.OutboxEvent;
import com.poc.common.contracts.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPollerService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollerService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventDispatcherService eventDispatcherService;

    @Value("${outbox.batch-size:50}")
    private int batchSize;

    @Value("${outbox.max-retries:5}")
    private int maxRetries;

    @Value("${topics.email:poc.dev.email.events.v1}")
    private String emailTopic;

    @Value("${topics.dlq.email:poc.dev.email.dlq.v1}")
    private String emailDlqTopic;

    public OutboxPollerService(OutboxEventRepository outboxEventRepository,
                        KafkaTemplate<String, String> kafkaTemplate,
                        EventDispatcherService eventDispatcherService) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.eventDispatcherService = eventDispatcherService;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:500}")
    public void poll() {
        List<OutboxEvent> events = claimEvents(batchSize);
        if (events.isEmpty()) {
            return;
        }
        log.debug("Claimed {} outbox events for processing", events.size());

        for (OutboxEvent event : events) {
            processEvent(event);
        }
    }

    @Transactional
    public List<OutboxEvent> claimEvents(int batchSize) {
        return outboxEventRepository.claimEvents(batchSize);
    }

    private void processEvent(OutboxEvent event) {
        try {
            String topic = resolveTopic(event.getEventType());
            String key = event.getSagaId() != null
                    ? event.getSagaId().toString()
                    : event.getAggregateId();

            kafkaTemplate.send(topic, key, event.getPayload()).get(5, TimeUnit.SECONDS);

            event.setPublished(true);
            event.setPublishedAt(java.time.Instant.now());
            outboxEventRepository.save(event);
            log.debug("Published event {} to topic {}", event.getId(), topic);
        } catch (Exception e) {
            handlePublishFailure(event, e);
        }
    }

    private void handlePublishFailure(OutboxEvent event, Exception e) {
        log.warn("Failed to publish event {}: {}", event.getId(), e.getMessage());

        if (event.getRetryCount() >= maxRetries) {
            log.error("Event {} exceeded max retries ({}), sending to DLQ", event.getId(), maxRetries);
            sendToDlq(event, e);
            event.setPublished(true);
            event.setPublishedAt(java.time.Instant.now());
            outboxEventRepository.save(event);
        } else {
            event.setRetryCount(event.getRetryCount() + 1);
            outboxEventRepository.save(event);
        }
    }

    private void sendToDlq(OutboxEvent event, Exception error) {
        try {
            String originalTopic = resolveTopic(event.getEventType());
            String dlqPayload = objectMapper.writeValueAsString(new DlqMessage(
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    originalTopic,
                    event.getPayload(),
                    error.getMessage(),
                    event.getRetryCount(),
                    java.time.Instant.now()
            ));
            kafkaTemplate.send(emailDlqTopic, event.getAggregateId(), dlqPayload);
        } catch (Exception ex) {
            log.error("Failed to send event {} to DLQ", event.getId(), ex);
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "EmailNotificationRequested" -> emailTopic;
            default -> emailTopic;
        };
    }

    public record DlqMessage(String aggregateType, String aggregateId, String eventType,
                              String originalTopic, String originalPayload, String error,
                              int retryCount, java.time.Instant failedAt) {}

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
}
