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

import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.data.client.junit.jupiter.OpenSearchRestTemplateConfiguration;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.index.query.InnerHitBuilder;
import org.opensearch.index.query.NestedQueryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.InnerHitsIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {InnerHitsORHLCIntegrationTests.Config.class})
public class InnerHitsORHLCIntegrationTests extends InnerHitsIntegrationTests {

    @Configuration
    @Import({OpenSearchRestTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("innerhits-os");
        }
    }

    @Override
    protected Query buildQueryForInnerHits(
            String innerHitName, String nestedQueryPath, String matchField, String matchValue) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        NestedQueryBuilder nestedQueryBuilder =
                nestedQuery(nestedQueryPath, matchQuery(matchField, matchValue), ScoreMode.Avg);
        nestedQueryBuilder.innerHit(new InnerHitBuilder(innerHitName));
        queryBuilder.withQuery(nestedQueryBuilder);

        return queryBuilder.build();
    }
}
