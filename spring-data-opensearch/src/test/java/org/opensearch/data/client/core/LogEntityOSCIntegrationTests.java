/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core;

import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.NativeQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.LogEntityIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {LogEntityOSCIntegrationTests.Config.class})
public class LogEntityOSCIntegrationTests extends LogEntityIntegrationTests {
    @Configuration
    @Import({OpenSearchTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("logentity-os");
        }
    }

    @Override
    public Query termQueryForIp(String ip) {
        return NativeQuery.builder() //
                .withQuery(qb -> qb //
                        .term(tq -> tq //
                                .field("ip") //
                                .value(FieldValue.of(ip))))
                .build();
    }

    @Override
    public Query rangeQueryForIp(String from, String to) {
        return NativeQuery.builder() //
                .withQuery(qb -> qb //
                        .range(rqb -> rqb //
                                .field("ip") //
                                .gte(JsonData.of(from))//
                                .lte(JsonData.of(to))//
                        )).build();
    }
}
