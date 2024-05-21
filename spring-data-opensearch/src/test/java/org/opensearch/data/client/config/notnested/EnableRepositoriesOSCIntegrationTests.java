/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.config.notnested;

import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.config.notnested.EnableRepositoriesIntegrationTests;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;

public class EnableRepositoriesOSCIntegrationTests extends EnableRepositoriesIntegrationTests {

    @Configuration
    @Import({OpenSearchTemplateConfiguration.class})
    @EnableElasticsearchRepositories(basePackages = {"org.springframework.data.elasticsearch.config.notnested"})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("repositories-os");
        }
    }
}
