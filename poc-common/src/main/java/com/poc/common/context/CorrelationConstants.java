package com.poc.common.context;

public final class CorrelationConstants {

    private CorrelationConstants() {}

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
}
