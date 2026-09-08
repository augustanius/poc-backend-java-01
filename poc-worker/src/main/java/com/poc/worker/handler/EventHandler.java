package com.poc.worker.handler;

import com.poc.common.outbox.OutboxEvent;

public interface EventHandler {

    String name();

    String eventType();

    void handle(OutboxEvent event);
}
