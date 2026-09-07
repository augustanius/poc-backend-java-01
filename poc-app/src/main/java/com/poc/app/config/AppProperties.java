package com.poc.app.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.datasource")
@Validated
public record AppProperties(
        @NotBlank(message = "spring.datasource.url or SPRING_DATASOURCE_URL env var is required")
        String url,

        @NotBlank(message = "spring.datasource.username or SPRING_DATASOURCE_USERNAME env var is required")
        String username,

        @NotBlank(message = "spring.datasource.password or SPRING_DATASOURCE_PASSWORD env var is required")
        String password
) {}
