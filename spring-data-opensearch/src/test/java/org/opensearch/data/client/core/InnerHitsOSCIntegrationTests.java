/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core;

import org.opensearch.client.opensearch._types.query_dsl.MatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.NestedQuery;
import org.opensearch.client.opensearch.core.search.InnerHits;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.NativeQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.InnerHitsIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {InnerHitsOSCIntegrationTests.Config.class})
public class InnerHitsOSCIntegrationTests extends InnerHitsIntegrationTests {

    @Configuration
    @Import({OpenSearchTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("innerhits-os");
        }
    }

    @Override
    protected Query buildQueryForInnerHits(
            String innerHitName, String nestedQueryPath, String matchField, String matchValue) {
        return NativeQuery.builder() //
                .withQuery(q -> q.nested( //
                        NestedQuery.of(n -> n //
                                .path(nestedQueryPath) //
                                .query(q2 -> q2.match( //
                                        MatchQuery.of(m -> m //
                                                .field(matchField) //
                                                .query(fv -> fv.stringValue(matchValue)) //
                                        ))) //
                                .innerHits(InnerHits.of(ih -> ih.name(innerHitName))) //
                        ))) //
                .build();
    }
}
