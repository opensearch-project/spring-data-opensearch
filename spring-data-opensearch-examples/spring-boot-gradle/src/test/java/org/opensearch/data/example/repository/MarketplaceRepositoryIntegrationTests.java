/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.data.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestHighLevelClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.data.OpenSearchDataAutoConfiguration;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@ImportAutoConfiguration(
        exclude = ElasticsearchDataAutoConfiguration.class,
        classes = {
            OpenSearchRestClientAutoConfiguration.class,
            OpenSearchRestHighLevelClientAutoConfiguration.class,
            OpenSearchDataAutoConfiguration.class
        })
@DataElasticsearchTest
@EnableElasticsearchRepositories(basePackageClasses = MarketplaceRepository.class)
@ContextConfiguration(initializers = MarketplaceRepositoryIntegrationTests.Initializer.class)
@Tag("integration-test")
public class MarketplaceRepositoryIntegrationTests {
    @Container
    static final OpensearchContainer opensearch = new OpensearchContainer("opensearchproject/opensearch:2.4.0")
            .withStartupAttempts(5)
            .withStartupTimeout(Duration.ofMinutes(2));

    @Test
    void testMarketplaceRepository(@Autowired MarketplaceRepository repository) {
        assertThat(repository.findAll()).hasSize(0);
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of("opensearch.uris=" + opensearch.getHttpHostAddress())
                    .applyTo(configurableApplicationContext.getEnvironment());
        }
    }
}
