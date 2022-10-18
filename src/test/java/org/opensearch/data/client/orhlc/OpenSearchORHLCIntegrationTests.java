/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import static org.assertj.core.api.Assertions.*;
import static org.opensearch.index.query.QueryBuilders.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.action.support.ActiveShardCount;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.common.lucene.search.function.CombineFunction;
import org.opensearch.common.lucene.search.function.FunctionScoreQuery;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.data.client.EnabledIfOpenSearchVersion;
import org.opensearch.data.client.junit.jupiter.OpenSearchRestTemplateConfiguration;
import org.opensearch.index.query.InnerHitBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.opensearch.index.query.functionscore.GaussDecayFunctionBuilder;
import org.opensearch.index.reindex.UpdateByQueryRequest;
import org.opensearch.join.query.ParentIdQueryBuilder;
import org.opensearch.script.Script;
import org.opensearch.search.collapse.CollapseBuilder;
import org.opensearch.search.fetch.subphase.FetchSourceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchIntegrationTests;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.ScriptType;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.IndicesOptions;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.RescorerQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {OpenSearchORHLCIntegrationTests.Config.class})
@DisplayName("Using OpenSearch RestHighLevelClient")
public class OpenSearchORHLCIntegrationTests extends ElasticsearchIntegrationTests {

    @Configuration
    @Import({OpenSearchRestTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("integration-os");
        }
    }

    @Override
    protected Query queryWithIds(String... ids) {
        return new NativeSearchQueryBuilder().withIds(ids).build();
    }

    @Override
    protected Query getTermQuery(String field, String value) {
        return new NativeSearchQueryBuilder().withQuery(termQuery(field, value)).build();
    }

    @Override
    protected BaseQueryBuilder<?, ?> getBuilderWithTermQuery(String field, String value) {
        return new NativeSearchQueryBuilder().withQuery(termQuery(field, value));
    }

    @Override
    protected BaseQueryBuilder<?, ?> getBuilderWithMatchQuery(String field, String value) {
        return new NativeSearchQueryBuilder().withQuery(matchQuery(field, value));
    }

    @Override
    protected BaseQueryBuilder<?, ?> getBuilderWithWildcardQuery(String field, String value) {
        return new NativeSearchQueryBuilder().withQuery(wildcardQuery(field, value));
    }

    @Override
    protected BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
        return new NativeSearchQueryBuilder().withQuery(matchAllQuery());
    }

    @Override
    protected Query getBoolQueryWithWildcardsFirstMustSecondShouldAndMinScore(
            String firstField, String firstValue, String secondField, String secondValue, float minScore) {
        return new NativeSearchQueryBuilder()
                .withQuery( //
                        boolQuery() //
                                .must(wildcardQuery(firstField, firstValue)) //
                                .should(wildcardQuery(secondField, secondValue))) //
                .withMinScore(minScore)
                .build();
    }

    @Override
    protected Query getQueryWithCollapse(String collapseField, @Nullable String innerHits, @Nullable Integer size) {
        CollapseBuilder collapseBuilder = new CollapseBuilder(collapseField);

        if (innerHits != null) {
            InnerHitBuilder innerHitBuilder = new InnerHitBuilder(innerHits);

            if (size != null) {
                innerHitBuilder.setSize(size);
            }

            collapseBuilder.setInnerHits(innerHitBuilder);
        }

        return new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withCollapseBuilder(collapseBuilder)
                .build();
    }

    @Override
    protected Query getMatchAllQueryWithFilterForId(String id) {
        return new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withFilter(boolQuery().filter(termQuery("id", id)))
                .build();
    }

    @Override
    protected Query getQueryForParentId(String type, String id, @Nullable String route) {

        NativeSearchQueryBuilder queryBuilder =
                new NativeSearchQueryBuilder().withQuery(new ParentIdQueryBuilder(type, id));

        if (route != null) {
            queryBuilder.withRoute(route);
        }
        return queryBuilder.build();
    }

    @Override
    protected Query getMatchAllQueryWithIncludesAndInlineExpressionScript(
            @Nullable String includes, String fieldName, String script, Map<String, java.lang.Object> params) {

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder().withQuery(matchAllQuery());

        if (includes != null) {
            nativeSearchQueryBuilder.withSourceFilter(
                    new FetchSourceFilterBuilder().withIncludes(includes).build());
        }
        return nativeSearchQueryBuilder
                .withScriptField(new ScriptField(
                        fieldName, new Script(org.opensearch.script.ScriptType.INLINE, "expression", script, params)))
                .build();
    }

    @Override
    protected Query getQueryWithRescorer() {
        return new NativeSearchQueryBuilder() //
                .withQuery( //
                        boolQuery() //
                                .filter(existsQuery("rate")) //
                                .should(termQuery("message", "message"))) //
                .withRescorerQuery( //
                        new RescorerQuery( //
                                        new NativeSearchQueryBuilder() //
                                                .withQuery(QueryBuilders.functionScoreQuery(
                                                                new FunctionScoreQueryBuilder.FilterFunctionBuilder[] {
                                                                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                                                            new GaussDecayFunctionBuilder(
                                                                                            "rate", 0, 10, null, 0.5)
                                                                                    .setWeight(1f)),
                                                                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                                                            new GaussDecayFunctionBuilder(
                                                                                            "rate", 0, 10, null, 0.5)
                                                                                    .setWeight(100f))
                                                                }) //
                                                        .scoreMode(FunctionScoreQuery.ScoreMode.SUM) //
                                                        .maxBoost(80f) //
                                                        .boostMode(CombineFunction.REPLACE)) //
                                                .build()) //
                                .withScoreMode(RescorerQuery.ScoreMode.Max) //
                                .withWindowSize(100)) //
                .build();
    }

    @Test // DATAES-768
    void shouldUseAllOptionsFromUpdateQuery() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", "1");
        doc.put("message", "test");
        org.springframework.data.elasticsearch.core.document.Document document =
                org.springframework.data.elasticsearch.core.document.Document.from(doc);
        UpdateQuery updateQuery = UpdateQuery.builder("1") //
                .withDocument(document) //
                .withIfSeqNo(42) //
                .withIfPrimaryTerm(13) //
                .withScript("script") //
                .withLang("lang") //
                .withRefreshPolicy(RefreshPolicy.WAIT_UNTIL) //
                .withRetryOnConflict(7) //
                .withTimeout("4711s") //
                .withWaitForActiveShards("all") //
                .withFetchSourceIncludes(Collections.singletonList("incl")) //
                .withFetchSourceExcludes(Collections.singletonList("excl")) //
                .build();

        UpdateRequest request = getRequestFactory().updateRequest(updateQuery, IndexCoordinates.of("index"));

        assertThat(request).isNotNull();
        assertThat(request.ifSeqNo()).isEqualTo(42);
        assertThat(request.ifPrimaryTerm()).isEqualTo(13);
        assertThat(request.script().getIdOrCode()).isEqualTo("script");
        assertThat(request.script().getLang()).isEqualTo("lang");
        assertThat(request.getRefreshPolicy()).isEqualByComparingTo(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        assertThat(request.retryOnConflict()).isEqualTo(7);
        assertThat(request.timeout()).isEqualByComparingTo(TimeValue.parseTimeValue("4711s", "test"));
        assertThat(request.waitForActiveShards()).isEqualTo(ActiveShardCount.ALL);
        FetchSourceContext fetchSourceContext = request.fetchSource();
        assertThat(fetchSourceContext).isNotNull();
        assertThat(fetchSourceContext.includes()).containsExactlyInAnyOrder("incl");
        assertThat(fetchSourceContext.excludes()).containsExactlyInAnyOrder("excl");
    }

    @Test // #1446, #2191
    void shouldUseAllOptionsFromUpdateByQuery() throws JSONException {

        Query searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery()) //
                .withIndicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN) //
                .build(); //
        searchQuery.setScrollTime(Duration.ofMillis(1000));

        UpdateQuery updateQuery = UpdateQuery.builder(searchQuery) //
                .withAbortOnVersionConflict(true) //
                .withBatchSize(10) //
                .withMaxDocs(12) //
                .withMaxRetries(3) //
                .withPipeline("pipeline") //
                .withRequestsPerSecond(5F) //
                .withShouldStoreResult(false) //
                .withSlices(4) //
                .withScriptType(ScriptType.INLINE) //
                .withScript("script") //
                .withLang("painless") //
                .build(); //

        String expectedSearchRequest = '{' + //
                "  \"size\": 10,"
                + //
                "  \"query\": {"
                + //
                "    \"match_all\": {"
                + //
                "      \"boost\": 1.0"
                + //
                "    }"
                + "  }" + '}';

        // when
        UpdateByQueryRequest request =
                getRequestFactory().updateByQueryRequest(updateQuery, IndexCoordinates.of("index1", "index2"));

        // then
        assertThat(request.indices()).containsExactlyInAnyOrder("index1", "index2");
        assertThat(request).isNotNull();
        assertThat(request.getSearchRequest().indicesOptions())
                .usingRecursiveComparison()
                .isEqualTo(IndicesOptions.LENIENT_EXPAND_OPEN);
        assertThat(request.getScrollTime().getMillis()).isEqualTo(1000);
        assertEquals(request.getSearchRequest().source().toString(), expectedSearchRequest, false);
        assertThat(request.isAbortOnVersionConflict()).isTrue();
        assertThat(request.getBatchSize()).isEqualTo(10);
        assertThat(request.getMaxDocs()).isEqualTo(12);
        assertThat(request.getPipeline()).isEqualTo("pipeline");
        assertThat(request.getRequestsPerSecond()).isEqualTo(5F);
        assertThat(request.getShouldStoreResult()).isFalse();
        assertThat(request.getSlices()).isEqualTo(4);
        assertThat(request.getScript().getIdOrCode()).isEqualTo("script");
        assertThat(request.getScript().getType()).isEqualTo(org.opensearch.script.ScriptType.INLINE);
        assertThat(request.getScript().getLang()).isEqualTo("painless");
    }

    @Test
    @EnabledIfOpenSearchVersion(
            onOrAfter = "2.0.0",
            reason = "https://github.com/opensearch-project/OpenSearch/issues/4749")
    @Override
    public void shouldReturnFailureInMultigetResult() {
        super.shouldReturnFailureInMultigetResult();
    }

    private RequestFactory getRequestFactory() {
        return ((OpenSearchRestTemplate) operations).getRequestFactory();
    }
}
