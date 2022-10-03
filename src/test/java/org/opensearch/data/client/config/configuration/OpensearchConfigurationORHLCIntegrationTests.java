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
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.client.orhlc.AbstractOpensearchConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests for {@link AbstractOpensearchConfiguration}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class OpensearchConfigurationORHLCIntegrationTests {

    /*
     * using a repository with an entity that is set to createIndex = false as we have no elastic running for this test
     * and just check that all the necessary beans are created.
     */
    @Autowired
    private CreateIndexFalseRepository repository;

    @Configuration
    @EnableElasticsearchRepositories(
            basePackages = {"org.opensearch.data.client.config.configuration"},
            considerNestedRepositories = true)
    static class Config extends AbstractOpensearchConfiguration {

        @Override
        public RestHighLevelClient opensearchClient() {
            return mock(RestHighLevelClient.class);
        }
    }

    @Test // DATAES-563
    public void bootstrapsRepository() {

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
