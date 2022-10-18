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
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.SearchAsYouTypeIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {SearchAsYouTypeORHLCIntegrationTests.Config.class})
public class SearchAsYouTypeORHLCIntegrationTests extends SearchAsYouTypeIntegrationTests {

    @Configuration
    @Import({OpenSearchRestTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("search-as-you-type-os");
        }
    }

    @Override
    protected Query buildMultiMatchQuery(String text) {
        return new NativeSearchQuery(QueryBuilders.multiMatchQuery(
                        text, //
                        "suggest",
                        "suggest._2gram",
                        "suggest._3gram",
                        "suggest._4gram")
                .type(MultiMatchQueryBuilder.Type.BOOL_PREFIX));
    }
}
