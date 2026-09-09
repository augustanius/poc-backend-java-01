package com.poc.worker.frameworks.handlers;

import com.poc.common.frameworks.outbox.OutboxEvent;

public interface EventHandler {

    String name();

    String eventType();

    void handle(OutboxEvent event);
}
