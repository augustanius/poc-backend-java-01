package com.poc.user.exception;

import com.poc.common.exception.ExternalServiceException;

import java.util.Map;

public class KeycloakUnavailableException extends ExternalServiceException {

    public KeycloakUnavailableException(String detail) {
        super("Identity provider unavailable: " + detail, Map.of("detail", detail));
    }

    public KeycloakUnavailableException(String detail, Throwable cause) {
        super("Identity provider unavailable: " + detail, Map.of("detail", detail), cause);
    }
}
