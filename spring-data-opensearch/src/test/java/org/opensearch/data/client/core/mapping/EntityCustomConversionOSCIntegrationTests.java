/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.mapping;

import java.util.Arrays;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.mapping.EntityCustomConversionIntegrationTests;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {EntityCustomConversionOSCIntegrationTests.Config.class})
public class EntityCustomConversionOSCIntegrationTests extends EntityCustomConversionIntegrationTests {

    @Configuration
    @Import({EntityCustomConversionIntegrationTests.Config.class})
    @EnableElasticsearchRepositories(
            basePackages = {"org.springframework.data.elasticsearch.core.mapping"},
            considerNestedRepositories = true)
    static class Config extends OpenSearchTemplateConfiguration {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("entity-customconversions-operations-os");
        }

        @Override
        public ElasticsearchCustomConversions elasticsearchCustomConversions() {
            return new ElasticsearchCustomConversions(
                    Arrays.asList(new EntityToMapConverter(), new MapToEntityConverter()));
        }
    }
}
