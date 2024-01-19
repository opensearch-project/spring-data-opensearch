/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.mapping;

import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.NativeQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.mapping.FieldNamingStrategyIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {FieldNamingStrategyOSCIntegrationTests.Config.class})
public class FieldNamingStrategyOSCIntegrationTests extends FieldNamingStrategyIntegrationTests {

    @Configuration
    static class Config extends OpenSearchTemplateConfiguration {

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
        return NativeQuery.builder() //
                .withQuery(q -> q.match(mq -> mq.field(fieldName).query(fv -> fv.stringValue(value)))).build();
    }
}
