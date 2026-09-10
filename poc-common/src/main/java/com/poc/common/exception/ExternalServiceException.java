package com.poc.common.exception;

import java.util.Map;

public class ExternalServiceException extends DomainException {

    public ExternalServiceException(String message, Map<String, Object> loggingContext) {
        super(message, loggingContext);
    }

    public ExternalServiceException(String message, Map<String, Object> loggingContext, Throwable cause) {
        super(message, loggingContext, cause);
    }
}
