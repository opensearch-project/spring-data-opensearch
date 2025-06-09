package org.opensearch.data.example.repository;

import org.opensearch.data.example.MarketplaceApplication;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

public class TestMarketplaceApplication {

    public static void main(String[] args) {
        SpringApplication.from(MarketplaceApplication::main)
                .with(ContainerConfig.class)
                .run(args);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerConfig {

        @Bean
        @ServiceConnection
        OpenSearchContainer<?> opensearchContainer() {
            return new OpenSearchContainer<>("opensearchproject/opensearch:3.1.0");
        }

    }
}
