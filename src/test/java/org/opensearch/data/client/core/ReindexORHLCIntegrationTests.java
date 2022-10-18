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
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ReindexIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {ReindexORHLCIntegrationTests.Config.class})
public class ReindexORHLCIntegrationTests extends ReindexIntegrationTests {

    @Configuration
    @Import({OpenSearchRestTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("reindex-os");
        }
    }

    @Override
    protected Query queryForId(String id) {
        return new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.termQuery("_id", id))
                .build();
    }
}
