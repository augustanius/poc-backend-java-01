package com.poc.worker.usecases;

import com.poc.common.frameworks.outbox.OutboxEvent;
import com.poc.worker.frameworks.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EventDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(EventDispatcherService.class);

    private final List<EventHandler> handlers;
    private final List<String> allowedHandlers;
    private Map<String, EventHandler> handlerMap;

    public EventDispatcherService(List<EventHandler> handlers,
                           @Value("${worker.handlers:*}") String allowedHandlersConfig) {
        this.handlers = handlers;
        this.allowedHandlers = "*".equals(allowedHandlersConfig.trim())
                ? List.of()
                : Arrays.asList(allowedHandlersConfig.split(","));
    }

    @PostConstruct
    public void init() {
        handlerMap = handlers.stream()
                .collect(Collectors.toMap(EventHandler::eventType, Function.identity()));

        log.info("EventDispatcher initialized with {} handlers: {}",
                handlers.size(),
                handlers.stream().map(EventHandler::name).toList());

        if (!allowedHandlers.isEmpty()) {
            log.info("Filtering to handlers: {}", allowedHandlers);
        }
    }

    public void dispatch(OutboxEvent event) {
        EventHandler handler = handlerMap.get(event.getEventType());
        if (handler == null) {
            log.debug("No handler for event type: {}", event.getEventType());
            return;
        }

        if (!allowedHandlers.isEmpty() && !allowedHandlers.contains(handler.name())) {
            log.debug("Handler '{}' not in allowed list, skipping", handler.name());
            return;
        }

        try {
            handler.handle(event);
        } catch (Exception e) {
            log.error("Handler '{}' failed for event {}: {}",
                    handler.name(), event.getId(), e.getMessage(), e);
            throw e;
        }
    }
}
