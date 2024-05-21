/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.repositories.dynamicindex;

import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.repositories.dynamicindex.DynamicIndexEntityIntegrationTests;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {DynamicIndexEntityOSCIntegrationTests.Config.class})
public class DynamicIndexEntityOSCIntegrationTests extends DynamicIndexEntityIntegrationTests {
    @Configuration
    @Import({OpenSearchTemplateConfiguration.class})
    @EnableElasticsearchRepositories(
            basePackages = {"org.springframework.data.elasticsearch.repositories.dynamicindex"},
            considerNestedRepositories = true)
    static class Config {
        @Bean
        public IndexNameProvider indexNameProvider() {
            return new IndexNameProvider();
        }
    }
}
