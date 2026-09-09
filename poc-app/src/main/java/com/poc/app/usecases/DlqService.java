package com.poc.app.usecases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.poc.app.contracts.usecases.ManageDlqUseCase;
import com.poc.app.frameworks.persistence.DlqMessageEntity;
import com.poc.app.frameworks.persistence.DlqMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DlqService implements ManageDlqUseCase {

    private static final Logger log = LoggerFactory.getLogger(DlqService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DlqMessageRepository dlqMessageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public DlqService(KafkaTemplate<String, String> kafkaTemplate,
                      DlqMessageRepository dlqMessageRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.dlqMessageRepository = dlqMessageRepository;
    }

    @Override
    @Transactional
    public DlqMessage addMessage(String topic, String key, String payload) {
        try {
            JsonNode json = parsePayload(payload);

            String originalTopic = extractField(json, "originalTopic");
            String eventType = extractField(json, "eventType");
            String errorMessage = extractField(json, "error");
            String originalPayload = extractField(json, "originalPayload");
            Instant failedAt = extractInstant(json, "failedAt");

            DlqMessageEntity entity = new DlqMessageEntity(
                    UUID.randomUUID().toString(),
                    topic,
                    originalTopic,
                    key,
                    eventType,
                    originalPayload != null ? originalPayload : payload,
                    errorMessage,
                    0,
                    Instant.now(),
                    failedAt
            );
            dlqMessageRepository.save(entity);
            log.debug("Stored DLQ message: id={}, topic={}, originalTopic={}",
                    entity.getId(), topic, originalTopic);
            return toDlqMessage(entity);
        } catch (Exception e) {
            log.error("Failed to store DLQ message: topic={}", topic, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqMessage> listAll() {
        return dlqMessageRepository.findAllByOrderByReceivedAtAsc().stream()
                .map(this::toDlqMessage)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqMessage> listByTopic(String topic) {
        return dlqMessageRepository.findByTopicOrderByReceivedAtAsc(topic).stream()
                .map(this::toDlqMessage)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DlqMessage getById(String id) {
        return dlqMessageRepository.findById(id).map(this::toDlqMessage).orElse(null);
    }

    @Override
    @Transactional
    public boolean retry(String id) {
        DlqMessageEntity entity = dlqMessageRepository.findById(id).orElse(null);
        if (entity == null) {
            log.warn("DLQ message not found: {}", id);
            return false;
        }

        try {
            String targetTopic = entity.getOriginalTopic() != null
                    ? entity.getOriginalTopic()
                    : "poc.dev.email.events.v1";
            String key = entity.getEventKey() != null ? entity.getEventKey() : "";
            String payload = entity.getPayload() != null ? entity.getPayload() : "";

            kafkaTemplate.send(targetTopic, key, payload).get();

            dlqMessageRepository.deleteById(id);
            log.info("Retried DLQ message: id={}, topic={}", id, targetTopic);
            return true;
        } catch (Exception e) {
            log.error("Failed to retry DLQ message: id={}", id, e);
            return false;
        }
    }

    @Override
    @Transactional
    public int retryAll() {
        List<DlqMessageEntity> all = dlqMessageRepository.findAllByOrderByReceivedAtAsc();
        int count = 0;

        for (DlqMessageEntity entity : all) {
            if (retry(entity.getId())) {
                count++;
            }
        }

        log.info("Retried {} DLQ messages out of {}", count, all.size());
        return count;
    }

    @Override
    @Transactional
    public int retryByTopic(String topic) {
        List<DlqMessageEntity> messages = dlqMessageRepository.findByTopicOrderByReceivedAtAsc(topic);
        int count = 0;

        for (DlqMessageEntity entity : messages) {
            if (retry(entity.getId())) {
                count++;
            }
        }

        log.info("Retried {} DLQ messages for topic {}", count, topic);
        return count;
    }

    @Override
    @Transactional
    public boolean remove(String id) {
        if (dlqMessageRepository.existsById(id)) {
            dlqMessageRepository.deleteById(id);
            log.info("Removed DLQ message: id={}", id);
            return true;
        }
        log.warn("DLQ message not found for removal: {}", id);
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        List<String> topics = dlqMessageRepository.findDistinctTopics();
        long total = dlqMessageRepository.count();

        Map<String, Long> topicCounts = topics.stream()
                .collect(Collectors.toMap(
                        topic -> topic,
                        dlqMessageRepository::countByTopic
                ));

        return Map.of(
                "dlqSize", total,
                "topics", topicCounts
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countByTopic(String topic) {
        return dlqMessageRepository.countByTopic(topic);
    }

    private DlqMessage toDlqMessage(DlqMessageEntity entity) {
        return new DlqMessage(
                entity.getId(),
                entity.getTopic(),
                entity.getOriginalTopic(),
                entity.getEventKey(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getErrorMessage(),
                entity.getRetryCount(),
                entity.getReceivedAt(),
                entity.getFailedAt()
        );
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private String extractField(JsonNode json, String field) {
        if (json.has(field) && !json.get(field).isNull()) {
            return json.get(field).asText();
        }
        return null;
    }

    private Instant extractInstant(JsonNode json, String field) {
        if (json.has(field) && !json.get(field).isNull()) {
            try {
                return Instant.parse(json.get(field).asText());
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
