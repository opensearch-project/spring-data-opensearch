/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.aggregation;

import static org.assertj.core.api.Assertions.*;
import static org.opensearch.index.query.QueryBuilders.*;
import static org.opensearch.search.aggregations.AggregationBuilders.*;
import static org.opensearch.search.aggregations.PipelineAggregatorBuilders.*;

import org.opensearch.action.search.SearchType;
import org.opensearch.data.client.junit.jupiter.OpenSearchRestTemplateConfiguration;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.client.orhlc.OpenSearchAggregations;
import org.opensearch.search.aggregations.Aggregation;
import org.opensearch.search.aggregations.Aggregations;
import org.opensearch.search.aggregations.pipeline.ParsedStatsBucket;
import org.opensearch.search.aggregations.pipeline.StatsBucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.aggregation.AggregationIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {AggregationORHLCIntegrationTests.Config.class})
public class AggregationORHLCIntegrationTests extends AggregationIntegrationTests {

    @Configuration
    @Import({OpenSearchRestTemplateConfiguration.class})
    @EnableElasticsearchRepositories(considerNestedRepositories = true)
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("aggs-es7");
        }
    }

    protected Query getTermsAggsQuery(String aggsName, String aggsField) {
        return new NativeSearchQueryBuilder() //
                .withQuery(matchAllQuery()) //
                .withSearchType(SearchType.DEFAULT) //
                .withAggregations(terms(aggsName).field(aggsField)) //
                .withMaxResults(0) //
                .build();
    }

    protected void assertThatAggsHasResult(AggregationsContainer<?> aggregationsContainer, String aggsName) {
        Aggregations aggregations = ((OpenSearchAggregations) aggregationsContainer).aggregations();
        assertThat(aggregations.asMap().get(aggsName)).isNotNull();
    }

    protected Query getPipelineAggsQuery(
            String aggsName, String aggsField, String aggsNamePipeline, String bucketsPath) {
        return new NativeSearchQueryBuilder() //
                .withQuery(matchAllQuery()) //
                .withSearchType(SearchType.DEFAULT) //
                .withAggregations(terms(aggsName).field(aggsField)) //
                .withPipelineAggregations(statsBucket(aggsNamePipeline, bucketsPath)) //
                .withMaxResults(0) //
                .build();
    }

    protected void assertThatPipelineAggsAreCorrect(
            AggregationsContainer<?> aggregationsContainer, String aggsName, String pipelineAggsName) {
        Aggregations aggregations = ((OpenSearchAggregations) aggregationsContainer).aggregations();

        assertThat(aggregations.asMap().get(aggsName)).isNotNull();
        Aggregation keyword_bucket_stats = aggregations.asMap().get(pipelineAggsName);
        assertThat(keyword_bucket_stats).isInstanceOf(StatsBucket.class);
        if (keyword_bucket_stats instanceof ParsedStatsBucket) {
            // Rest client
            ParsedStatsBucket statsBucket = (ParsedStatsBucket) keyword_bucket_stats;
            assertThat(statsBucket.getMin()).isEqualTo(1.0);
            assertThat(statsBucket.getMax()).isEqualTo(3.0);
            assertThat(statsBucket.getAvg()).isEqualTo(2.0);
            assertThat(statsBucket.getSum()).isEqualTo(6.0);
            assertThat(statsBucket.getCount()).isEqualTo(3L);
        }
    }
}
