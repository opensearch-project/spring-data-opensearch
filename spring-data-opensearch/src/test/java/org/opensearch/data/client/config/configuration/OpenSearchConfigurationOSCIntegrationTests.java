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
import org.opensearch.client.RestClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.data.client.osc.OpenSearchConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests for {@link OpenSearchConfiguration}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class OpenSearchConfigurationOSCIntegrationTests {

    /*
     * using a repository with an entity that is set to createIndex = false as we have no elastic running for this test
     * and just check that all the necessary beans are created.
     */
    @Autowired private RestClient restClient;
    @Autowired private OpenSearchClient opensearchClient;
    @Autowired private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private CreateIndexFalseRepository repository;

    @Configuration
    @EnableElasticsearchRepositories(
            basePackages = {"org.opensearch.data.client.config.configuration"},
            considerNestedRepositories = true)
    static class Config extends OpenSearchConfiguration {

        @NonNull
        @Override
        public ClientConfiguration clientConfiguration() {
            return ClientConfiguration.builder() //
                    .connectedTo("localhost:9200") //
                    .build();
        }
    }

    @Test
    public void providesRequiredBeans() {
        assertThat(restClient).isNotNull();
        assertThat(opensearchClient).isNotNull();
        assertThat(elasticsearchOperations).isNotNull();
        assertThat(repository).isNotNull();
    }

    @Document(indexName = "test-index-config-configuration", createIndex = false)
    static class CreateIndexFalseEntity {

        @Nullable
        @Id
        private String id;
    }

    interface CreateIndexFalseRepository extends ElasticsearchRepository<CreateIndexFalseEntity, String> {}
}
