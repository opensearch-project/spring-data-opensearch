/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core;


import org.opensearch.data.client.junit.jupiter.OpenSearchRestTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.SourceFilterIntegrationTests;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {SourceFilterORHLCIntegrationTests.Config.class})
public class SourceFilterORHLCIntegrationTests extends SourceFilterIntegrationTests {

    @Configuration
    @Import({OpenSearchRestTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("source-filter-os");
        }
    }
}
