/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.config.configuration;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests for {@link ReactiveOpenSearchConfiguration}.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class ReactiveOpenSearchConfigurationOSCTests {

    @Configuration
    @EnableReactiveElasticsearchRepositories(
            basePackages = { "org.opensearch.data.client.config.configuration" },
            considerNestedRepositories = true)
    static class Config extends ReactiveOpenSearchConfiguration {

        @Override
        public ClientConfiguration clientConfiguration() {
            return ClientConfiguration.builder() //
                    .connectedTo("localhost:9200") //
                    .build();
        }
    }

    /*
     * using a repository with an entity that is set to createIndex = false as we have no elastic running for this test
     * and just check that all the necessary beans are created.
     */
    @Autowired private ReactiveOpenSearchClient reactiveOpenSearchClient;
    @Autowired private ReactiveElasticsearchOperations reactiveElasticsearchOperations;
    @Autowired private CreateIndexFalseRepository repository;

    @Test
    public void providesRequiredBeans() {
        // assertThat(webClient).isNotNull();
        assertThat(reactiveOpenSearchClient).isNotNull();
        assertThat(reactiveElasticsearchOperations).isNotNull();
        assertThat(repository).isNotNull();
    }

    @Document(indexName = "test-index-config-configuration", createIndex = false)
    static class CreateIndexFalseEntity {

        @Nullable
        @Id private String id;
    }

    interface CreateIndexFalseRepository extends ReactiveElasticsearchRepository<CreateIndexFalseEntity, String> {}
}
