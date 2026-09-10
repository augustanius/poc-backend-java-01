package com.poc.common.exception;

import java.util.Map;

public class ConflictException extends DomainException {

    public ConflictException(String message, Map<String, Object> loggingContext) {
        super(message, loggingContext);
    }

    public ConflictException(String message, Map<String, Object> loggingContext, Throwable cause) {
        super(message, loggingContext, cause);
    }
}
