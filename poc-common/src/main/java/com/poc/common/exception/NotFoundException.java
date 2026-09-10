package com.poc.common.exception;

import java.util.Map;

public class NotFoundException extends DomainException {

    public NotFoundException(String message, Map<String, Object> loggingContext) {
        super(message, loggingContext);
    }

    public NotFoundException(String message, Map<String, Object> loggingContext, Throwable cause) {
        super(message, loggingContext, cause);
    }
}
