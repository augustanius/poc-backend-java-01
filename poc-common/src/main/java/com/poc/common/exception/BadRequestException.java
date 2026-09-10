package com.poc.common.exception;

import java.util.Map;

public class BadRequestException extends DomainException {

    public BadRequestException(String message, Map<String, Object> loggingContext) {
        super(message, loggingContext);
    }

    public BadRequestException(String message, Map<String, Object> loggingContext, Throwable cause) {
        super(message, loggingContext, cause);
    }
}
