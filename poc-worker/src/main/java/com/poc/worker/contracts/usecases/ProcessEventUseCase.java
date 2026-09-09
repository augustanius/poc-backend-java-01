package com.poc.worker.contracts.usecases;

import com.poc.common.frameworks.outbox.OutboxEvent;

public interface ProcessEventUseCase {

    void process(OutboxEvent event);
}
