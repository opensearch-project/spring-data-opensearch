/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core;

import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.NativeQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.SearchAsYouTypeIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {SearchAsYouTypeOSCIntegrationTests.Config.class})
public class SearchAsYouTypeOSCIntegrationTests extends SearchAsYouTypeIntegrationTests {

    @Configuration
    @Import({OpenSearchTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("search-as-you-type-os");
        }
    }

    @Override
    protected Query buildMultiMatchQuery(String text) {
        return NativeQuery.builder() //
                .withQuery(q -> q //
                        .multiMatch(mm -> mm //
                                .query(text) //
                                .fields("suggest", "suggest._2gram", "suggest._3gram", "suggest._4gram") //
                                .type(TextQueryType.BoolPrefix)))
                .build();
    }
}
