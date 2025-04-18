/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.data.example.repository;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opensearch.spring.boot.autoconfigure.test.DataOpenSearchTest;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

@Testcontainers(disabledWithoutDocker = true)
@DataOpenSearchTest
@EnableReactiveElasticsearchRepositories(basePackageClasses = ReactiveMarketplaceRepository.class)
@Tag("integration-test")
public class MarketplaceRepositoryIntegrationTests {
    @Container
    @ServiceConnection
    static final OpensearchContainer<?> opensearch = new OpensearchContainer<>("opensearchproject/opensearch:2.19.1")
            .withStartupAttempts(5)
            .withStartupTimeout(Duration.ofMinutes(2));

    @Test
    void testMarketplaceRepository(@Autowired ReactiveMarketplaceRepository repository) {
        StepVerifier
            .create(repository.findAll())
            .expectComplete()
            .verify(Duration.ofSeconds(5));
    }
}
