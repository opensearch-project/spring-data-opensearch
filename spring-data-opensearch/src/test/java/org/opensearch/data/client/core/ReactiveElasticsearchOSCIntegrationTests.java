/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Buckets;
import org.opensearch.client.opensearch._types.aggregations.StringTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.search.FieldCollapse;
import org.opensearch.data.client.EnabledIfOpenSearchVersion;
import org.opensearch.data.client.junit.jupiter.ReactiveOpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.Aggregation;
import org.opensearch.data.client.osc.NativeQuery;
import org.opensearch.data.client.osc.OpenSearchAggregation;
import org.opensearch.data.client.osc.Queries;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.AggregationContainer;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchIntegrationTests;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = ReactiveElasticsearchOSCIntegrationTests.Config.class)
public class ReactiveElasticsearchOSCIntegrationTests extends ReactiveElasticsearchIntegrationTests {

    @Test // #2745
    @DisplayName("should use sort defined in native unbounded query")
    @EnabledIfOpenSearchVersion(
            onOrAfter = "2.3.0",
            reason = "https://github.com/opensearch-project/OpenSearch/issues/1147")
    void shouldUseSortDefinedInNativeUnboundedQuery() {
        var entity1 = randomEntity(null);
        entity1.setRate(7);
        var entity2 = randomEntity(null);
        entity2.setRate(5);
        var entity3 = randomEntity(null);
        entity3.setRate(11);

        operations.saveAll(List.of(entity1, entity2, entity3), SampleEntity.class).blockLast();

        var query = NativeQuery.builder()
                .withQuery(qb -> qb
                        .matchAll(m -> m))
                .withSort(sob -> sob
                        .field(f -> f
                                .field("rate")
                                .order(SortOrder.Asc)))
                .withPageable(Pageable.unpaged())
                .build();

        var rates = operations.search(query, SampleEntity.class)
                .map(SearchHit::getContent)
                .map(SampleEntity::getRate)
                .collectList().block();
        assertThat(rates).containsExactly(5, 7, 11);

        query = NativeQuery.builder()
                .withQuery(qb -> qb
                        .matchAll(m -> m))
                .withSort(sob -> sob
                        .field(f -> f
                                .field("rate")
                                .order(SortOrder.Desc)))
                .withPageable(Pageable.unpaged())
                .build();

        rates = operations.search(query, SampleEntity.class)
                .map(SearchHit::getContent)
                .map(SampleEntity::getRate)
                .collectList().block();
        assertThat(rates).containsExactly(11, 7, 5);
    }

    @Configuration
    @Import({ ReactiveOpenSearchTemplateConfiguration.class })
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("reactive-template-os");
        }
    }

    @Override
    protected Query getTermsAggsQuery(String aggsName, String aggsField) {
        return Queries.getTermsAggsQuery(aggsName, aggsField);
    }

    @Override
    protected BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
        return Queries.getBuilderWithMatchAllQuery();
    }

    @Override
    protected BaseQueryBuilder<?, ?> getBuilderWithTermQuery(String field, String value) {
        return Queries.getBuilderWithTermQuery(field, value);
    }

    @Override
    protected Query getQueryWithCollapse(String collapseField, @Nullable String innerHits, @Nullable Integer size) {
        return NativeQuery.builder() //
                .withQuery(Queries.matchAllQueryAsQuery()) //
                .withFieldCollapse(FieldCollapse.of(fc -> {
                    fc.field(collapseField);

                    if (innerHits != null) {
                        fc.innerHits(ih -> ih.name(innerHits).size(size));
                    }
                    return fc;
                })).build();
    }

    @Override
    protected Query queryWithIds(String... ids) {
        return Queries.queryWithIds(ids);
    }

    @Override
    protected <A extends AggregationContainer<?>> void assertThatAggregationsAreCorrect(A aggregationContainer) {
        Aggregation aggregation = ((OpenSearchAggregation) aggregationContainer).aggregation();
        assertThat(aggregation.getName()).isEqualTo("messages");
        Aggregate aggregate = aggregation.getAggregate();
        assertThat(aggregate.isSterms()).isTrue();
        StringTermsAggregate parsedStringTerms = (StringTermsAggregate) aggregate.sterms();
        Buckets<StringTermsBucket> buckets = parsedStringTerms.buckets();
        assertThat(buckets.isArray()).isTrue();
        List<StringTermsBucket> bucketList = buckets.array();
        assertThat(bucketList.size()).isEqualTo(3);
        AtomicInteger count = new AtomicInteger();
        bucketList.forEach(stringTermsBucket -> {
            if ("message".equals(stringTermsBucket.key())) {
                count.getAndIncrement();
                assertThat(stringTermsBucket.docCount()).isEqualTo(3);
            }
            if ("some".equals(stringTermsBucket.key())) {
                count.getAndIncrement();
                assertThat(stringTermsBucket.docCount()).isEqualTo(2);
            }
            if ("other".equals(stringTermsBucket.key())) {
                count.getAndIncrement();
                assertThat(stringTermsBucket.docCount()).isEqualTo(1);
            }
        });
        assertThat(count.get()).isEqualTo(3);
    }
}
