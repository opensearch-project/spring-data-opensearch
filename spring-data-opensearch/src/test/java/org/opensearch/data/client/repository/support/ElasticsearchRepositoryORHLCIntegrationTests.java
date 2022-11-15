/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.repository.support;

import org.opensearch.data.client.junit.jupiter.OpenSearchRestTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchRepositoryIntegrationTests;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {ElasticsearchRepositoryORHLCIntegrationTests.Config.class})
public class ElasticsearchRepositoryORHLCIntegrationTests extends ElasticsearchRepositoryIntegrationTests {

    @Configuration
    @Import({OpenSearchRestTemplateConfiguration.class})
    @EnableElasticsearchRepositories(
            basePackages = {"org.springframework.data.elasticsearch.repository.support"},
            considerNestedRepositories = true)
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("repository-os");
        }
    }
}
