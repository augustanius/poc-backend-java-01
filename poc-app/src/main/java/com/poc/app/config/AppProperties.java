package com.poc.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.datasource")
@Validated
public record AppProperties(
        Primary primary,
        Replica replica
) {
    public record Primary(
            String url,
            String username,
            String password
    ) {}

    public record Replica(
            String url,
            String username,
            String password
    ) {}
}
