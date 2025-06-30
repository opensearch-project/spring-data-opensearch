/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.data.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opensearch.spring.boot.autoconfigure.test.DataOpenSearchTest;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DataOpenSearchTest
@EnableElasticsearchRepositories(basePackageClasses = MarketplaceRepository.class)
@Tag("integration-test")
public class MarketplaceRepositoryIntegrationTests {
    @Container
    @ServiceConnection
    static final OpenSearchContainer<?> opensearch = new OpenSearchContainer<>("opensearchproject/opensearch:3.1.0")
            .withStartupAttempts(5)
            .withStartupTimeout(Duration.ofMinutes(2));

    @Test
    void testMarketplaceRepository(@Autowired MarketplaceRepository repository) {
        assertThat(repository.findAll()).isEmpty();
    }
}
