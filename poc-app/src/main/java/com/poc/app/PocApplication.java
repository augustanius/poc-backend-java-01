package com.poc.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.poc")
@EnableJpaRepositories(basePackages = "com.poc")
@EntityScan(basePackages = "com.poc")
public class PocApplication {

    public static void main(String[] args) {
        SpringApplication.run(PocApplication.class, args);
    }
}
