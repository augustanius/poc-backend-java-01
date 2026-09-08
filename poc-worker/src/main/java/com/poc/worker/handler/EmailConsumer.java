package com.poc.worker.handler;

import com.poc.common.outbox.OutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailConsumer.class);

    private final EventDispatcher eventDispatcher;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String emailTopic;
    private final String dlqTopic;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public EmailConsumer(EventDispatcher eventDispatcher,
                         KafkaTemplate<String, String> kafkaTemplate,
                         @Value("${topics.email:poc.dev.email.events.v1}") String emailTopic,
                         @Value("${topics.dlq.email:poc.dev.email.dlq.v1}") String dlqTopic) {
        this.eventDispatcher = eventDispatcher;
        this.kafkaTemplate = kafkaTemplate;
        this.emailTopic = emailTopic;
        this.dlqTopic = dlqTopic;
    }

    @KafkaListener(topics = "${topics.email:poc.dev.email.events.v1}", groupId = "poc-email-consumer")
    public void onMessage(String payload) {
        log.debug("Received Kafka message: {}", payload);

        OutboxEvent event = new OutboxEvent(
                null,
                "Notification",
                "",
                "EmailNotificationRequested",
                payload,
                null,
                java.time.Instant.now()
        );

        try {
            eventDispatcher.dispatch(event);
        } catch (Exception e) {
            log.error("Failed to process email event: {}", payload, e);
            sendToDlq(payload, e);
        }
    }

    private void sendToDlq(String originalPayload, Exception error) {
        try {
            String dlqPayload = objectMapper.writeValueAsString(new OutboxPoller.DlqMessage(
                    "Notification",
                    "",
                    "EmailNotificationRequested",
                    emailTopic,
                    originalPayload,
                    error.getMessage(),
                    0,
                    java.time.Instant.now()
            ));
            kafkaTemplate.send(dlqTopic, "", dlqPayload);
            log.info("Sent failed email event to DLQ topic: {}", dlqTopic);
        } catch (Exception ex) {
            log.error("Failed to send event to DLQ: {}", ex.getMessage(), ex);
        }
    }
}
