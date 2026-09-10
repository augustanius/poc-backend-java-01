package com.poc.common.exception;

import java.util.Map;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message, Map<String, Object> loggingContext) {
        super(message, loggingContext);
    }

    public UnauthorizedException(String message, Map<String, Object> loggingContext, Throwable cause) {
        super(message, loggingContext, cause);
    }
}
