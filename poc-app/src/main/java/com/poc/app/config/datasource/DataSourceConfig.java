package com.poc.app.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("app.datasource.primary.hikari")
    public HikariDataSource primaryDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties("app.datasource.replica.hikari")
    public HikariDataSource replicaDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public DataSource routingDataSource(
            @Qualifier("primaryDataSource")
            HikariDataSource primaryDataSource,

            @Qualifier("replicaDataSource")
            HikariDataSource replicaDataSource) {

        RoutingDataSource routingDataSource =
                new RoutingDataSource();

        routingDataSource.setTargetDataSources(Map.of(
                DataSourceRoutingContext.DataSourceType.PRIMARY,
                primaryDataSource,

                DataSourceRoutingContext.DataSourceType.REPLICA,
                replicaDataSource
        ));

        routingDataSource.setDefaultTargetDataSource(
                primaryDataSource
        );

        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }

    @Bean
    @Primary
    public DataSource dataSource(
            @Qualifier("routingDataSource")
            DataSource routingDataSource) {

        return new LazyConnectionDataSourceProxy(
                routingDataSource
        );
    }
}