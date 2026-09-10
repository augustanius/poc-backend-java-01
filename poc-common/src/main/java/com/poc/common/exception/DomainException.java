package com.poc.common.exception;

import java.util.Map;

public abstract class DomainException extends RuntimeException {

    private final Map<String, Object> loggingContext;

    protected DomainException(String message, Map<String, Object> loggingContext) {
        super(message);
        this.loggingContext = loggingContext != null ? Map.copyOf(loggingContext) : Map.of();
    }

    protected DomainException(String message, Map<String, Object> loggingContext, Throwable cause) {
        super(message, cause);
        this.loggingContext = loggingContext != null ? Map.copyOf(loggingContext) : Map.of();
    }

    public Map<String, Object> getLoggingContext() {
        return loggingContext;
    }
}
