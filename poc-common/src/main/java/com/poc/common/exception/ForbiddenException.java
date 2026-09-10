package com.poc.common.exception;

import java.util.Map;

public class ForbiddenException extends DomainException {

    public ForbiddenException(String message, Map<String, Object> loggingContext) {
        super(message, loggingContext);
    }

    public ForbiddenException(String message, Map<String, Object> loggingContext, Throwable cause) {
        super(message, loggingContext, cause);
    }
}
