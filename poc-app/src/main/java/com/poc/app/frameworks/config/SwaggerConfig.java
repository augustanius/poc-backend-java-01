package com.poc.app.frameworks.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class SwaggerConfig {

    @Bean
    public OpenAPI pocOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PoC Modular Monolith API")
                        .description("Modular monolith with hexagonal architecture")
                        .version("0.0.1")
                        .license(new License().name("MIT")));
    }
}
