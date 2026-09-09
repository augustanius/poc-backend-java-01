package com.poc.app.contracts.usecases;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface ManageDlqUseCase {

    record DlqMessage(String id, String topic, String originalTopic, String eventKey,
                       String eventType, String payload, String error,
                       int retryCount, Instant receivedAt, Instant failedAt) {}

    DlqMessage addMessage(String topic, String key, String payload);

    List<DlqMessage> listAll();

    List<DlqMessage> listByTopic(String topic);

    DlqMessage getById(String id);

    boolean retry(String id);

    int retryAll();

    int retryByTopic(String topic);

    boolean remove(String id);

    Map<String, Object> getStats();

    long countByTopic(String topic);
}
