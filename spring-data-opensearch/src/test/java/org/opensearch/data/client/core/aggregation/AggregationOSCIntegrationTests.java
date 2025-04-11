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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.StatsBucketAggregate;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.NativeQuery;
import org.opensearch.data.client.osc.OpenSearchAggregation;
import org.opensearch.data.client.osc.OpenSearchAggregations;
import org.opensearch.data.client.osc.Queries;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.aggregation.AggregationIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {AggregationOSCIntegrationTests.Config.class})
public class AggregationOSCIntegrationTests extends AggregationIntegrationTests {

    @Configuration
    @Import({OpenSearchTemplateConfiguration.class})
    @EnableElasticsearchRepositories(considerNestedRepositories = true)
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("aggs-es7");
        }
    }

    protected Query getTermsAggsQuery(String aggsName, String aggsField) {
        return Queries.getTermsAggsQuery(aggsName, aggsField);
    }

    @Override
    protected void assertThatAggsHasResult(AggregationsContainer<?> aggregationsContainer, String aggsName) {
        List<OpenSearchAggregation> aggregations = ((OpenSearchAggregations) aggregationsContainer).aggregations();
        List<String> aggNames = aggregations.stream() //
                .map(OpenSearchAggregation::aggregation) //
                .map(org.opensearch.data.client.osc.Aggregation::name) //
                .collect(Collectors.toList());
        assertThat(aggNames).contains(aggsName);

    }

    protected Query getPipelineAggsQuery(
            String aggsName, String aggsField, String aggsNamePipeline, String bucketsPath) {
        return NativeQuery.builder() //
                .withQuery(Queries.matchAllQueryAsQuery()) //
                .withAggregation(aggsName, Aggregation.of(a -> a //
                        .terms(ta -> ta.field(aggsField)))) //
                .withAggregation(aggsNamePipeline, Aggregation.of(a -> a //
                        .statsBucket(sb -> sb.bucketsPath(bp -> bp.single(bucketsPath))))) //
                .withMaxResults(0) //
                .build();

    }

    protected void assertThatPipelineAggsAreCorrect(
            AggregationsContainer<?> aggregationsContainer, String aggsName, String pipelineAggsName) {
        Map<String, Aggregate> aggregates = ((OpenSearchAggregations) aggregationsContainer).aggregations().stream() //
                .map(OpenSearchAggregation::aggregation) //
                .collect(Collectors.toMap(org.opensearch.data.client.osc.Aggregation::name,
                        org.opensearch.data.client.osc.Aggregation::aggregate));

        assertThat(aggregates).containsKey(aggsName);
        Aggregate aggregate = aggregates.get(pipelineAggsName);
        assertThat(aggregate.isStatsBucket()).isTrue();
        StatsBucketAggregate statsBucketAggregate = aggregate.statsBucket();
        assertThat(statsBucketAggregate.min()).isEqualTo(1.0);
        assertThat(statsBucketAggregate.max()).isEqualTo(3.0);
        assertThat(statsBucketAggregate.avg()).isEqualTo(2.0);
        assertThat(statsBucketAggregate.sum()).isEqualTo(6.0);
        assertThat(statsBucketAggregate.count()).isEqualTo(3L);
    }
}
