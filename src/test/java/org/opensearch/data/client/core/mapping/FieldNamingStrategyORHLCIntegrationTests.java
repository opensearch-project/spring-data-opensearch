/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.mapping;

import static org.opensearch.index.query.QueryBuilders.*;

import org.opensearch.data.client.junit.jupiter.OpensearchRestTemplateConfiguration;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.mapping.FieldNamingStrategyIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {FieldNamingStrategyORHLCIntegrationTests.Config.class})
public class FieldNamingStrategyORHLCIntegrationTests extends FieldNamingStrategyIntegrationTests {

    @Configuration
    static class Config extends OpensearchRestTemplateConfiguration {

        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("fieldnaming-strategy-os");
        }

        @Override
        protected FieldNamingStrategy fieldNamingStrategy() {
            return new SnakeCaseFieldNamingStrategy();
        }
    }

    @Override
    protected Query nativeMatchQuery(String fieldName, String value) {
        return new NativeSearchQueryBuilder()
                .withQuery(matchQuery(fieldName, value))
                .build();
    }
}
