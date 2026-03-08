/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.index;

import org.junit.jupiter.api.Disabled;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.index.MappingBuilderIntegrationTests;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {MappingBuilderOSCIntegrationTests.Config.class})
public class MappingBuilderOSCIntegrationTests extends MappingBuilderIntegrationTests {

    @Configuration
    @Import({OpenSearchTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("mappingbuilder-os");
        }
    }

    @Disabled
    @Override
    public void shouldWriteRuntimeFields() {
        // Not supported by OpenSearch
    }

    @Disabled
    @Override
    public void shouldWriteWildcardFieldMapping() {
        // Not supported by OpenSearch
    }
}
