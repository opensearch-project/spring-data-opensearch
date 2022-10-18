/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core;

import static org.opensearch.index.query.QueryBuilders.*;

import org.opensearch.data.client.junit.jupiter.OpenSearchRestTemplateConfiguration;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.LogEntityIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {LogEntityORHLCIntegrationTests.Config.class})
public class LogEntityORHLCIntegrationTests extends LogEntityIntegrationTests {
    @Configuration
    @Import({OpenSearchRestTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("logentity-os");
        }
    }

    @Override
    public Query termQueryForIp(String ip) {
        return new NativeSearchQueryBuilder().withQuery(termQuery("ip", ip)).build();
    }

    @Override
    public Query rangeQueryForIp(String from, String to) {
        return new NativeSearchQueryBuilder()
                .withQuery(rangeQuery("ip").from("10.10.10.1").to("10.10.10.3"))
                .build();
    }
}
