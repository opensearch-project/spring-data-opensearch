/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.script.mustache.SearchTemplateRequestBuilder;
import org.opensearch.search.SearchExtBuilder;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.search.aggregations.PipelineAggregationBuilder;
import org.opensearch.search.collapse.CollapseBuilder;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.search.suggest.SuggestBuilder;
import org.springframework.data.elasticsearch.core.query.BaseQuery;
import org.springframework.data.elasticsearch.core.query.IndexBoost;
import org.springframework.lang.Nullable;

/**
 * A query created from OpenSearch QueryBuilder instances. Note: the filter constructor parameter is used to create a
 * post_filter
 * {@see https://www.elastic.co/guide/en/elasticsearch/reference/7.10/filter-search-results.html#post-filter}, if a
 * filter is needed that filters before aggregations are build, it must be included in the query constructor parameter.
 * @since 0.1
 */
public class NativeSearchQuery extends BaseQuery {

    @Nullable
    private final QueryBuilder query;

    @Nullable
    private QueryBuilder filter;

    @Nullable
    private List<SortBuilder<?>> sorts;

    private final List<ScriptField> scriptFields = new ArrayList<>();

    @Nullable
    private CollapseBuilder collapseBuilder;

    @Nullable
    private List<AbstractAggregationBuilder<?>> aggregations;

    @Nullable
    private List<PipelineAggregationBuilder> pipelineAggregations;

    @Nullable
    private HighlightBuilder highlightBuilder;

    @Nullable
    private HighlightBuilder.Field[] highlightFields;

    @Nullable
    private SearchTemplateRequestBuilder searchTemplate;

    @Nullable
    private SuggestBuilder suggestBuilder;

    @Nullable
    private List<SearchExtBuilder> searchExtBuilders;

    public NativeSearchQuery(@Nullable QueryBuilder query) {
        this.query = query;
    }

    public NativeSearchQuery(@Nullable QueryBuilder query, @Nullable QueryBuilder filter) {

        this.query = query;
        this.filter = filter;
    }

    public NativeSearchQuery(
            @Nullable QueryBuilder query, @Nullable QueryBuilder filter, @Nullable List<SortBuilder<?>> sorts) {

        this.query = query;
        this.filter = filter;
        this.sorts = sorts;
    }

    public NativeSearchQuery(
            @Nullable QueryBuilder query,
            @Nullable QueryBuilder filter,
            @Nullable List<SortBuilder<?>> sorts,
            @Nullable HighlightBuilder.Field[] highlightFields) {

        this.query = query;
        this.filter = filter;
        this.sorts = sorts;
        this.highlightFields = highlightFields;
    }

    public NativeSearchQuery(
            @Nullable QueryBuilder query,
            @Nullable QueryBuilder filter,
            @Nullable List<SortBuilder<?>> sorts,
            @Nullable HighlightBuilder highlightBuilder,
            @Nullable HighlightBuilder.Field[] highlightFields) {

        this.query = query;
        this.filter = filter;
        this.sorts = sorts;
        this.highlightBuilder = highlightBuilder;
        this.highlightFields = highlightFields;
    }

    public NativeSearchQuery(
            NativeSearchQueryBuilder builder,
            @Nullable QueryBuilder query,
            @Nullable QueryBuilder filter,
            @Nullable List<SortBuilder<?>> sorts,
            @Nullable HighlightBuilder highlightBuilder,
            @Nullable HighlightBuilder.Field[] highlightFields) {
        super(builder);
        this.query = query;
        this.filter = filter;
        this.sorts = sorts;
        this.highlightBuilder = highlightBuilder;
        this.highlightFields = highlightFields;
    }

    @Nullable
    public QueryBuilder getQuery() {
        return query;
    }

    @Nullable
    public QueryBuilder getFilter() {
        return filter;
    }

    @Nullable
    public List<SortBuilder<?>> getOpenSearchSorts() {
        return sorts;
    }

    @Nullable
    public HighlightBuilder getHighlightBuilder() {
        return highlightBuilder;
    }

    @Nullable
    public HighlightBuilder.Field[] getHighlightFields() {
        return highlightFields;
    }

    public List<ScriptField> getScriptFields() {
        return scriptFields;
    }

    public void setScriptFields(List<ScriptField> scriptFields) {
        this.scriptFields.addAll(scriptFields);
    }

    public void addScriptField(ScriptField... scriptField) {
        scriptFields.addAll(Arrays.asList(scriptField));
    }

    @Nullable
    public CollapseBuilder getCollapseBuilder() {
        return collapseBuilder;
    }

    public void setCollapseBuilder(CollapseBuilder collapseBuilder) {
        this.collapseBuilder = collapseBuilder;
    }

    @Nullable
    public List<AbstractAggregationBuilder<?>> getAggregations() {
        return aggregations;
    }

    @Nullable
    public List<PipelineAggregationBuilder> getPipelineAggregations() {
        return pipelineAggregations;
    }

    public void addAggregation(AbstractAggregationBuilder<?> aggregationBuilder) {

        if (aggregations == null) {
            aggregations = new ArrayList<>();
        }

        aggregations.add(aggregationBuilder);
    }

    public void setAggregations(List<AbstractAggregationBuilder<?>> aggregations) {
        this.aggregations = aggregations;
    }

    public void setPipelineAggregations(List<PipelineAggregationBuilder> pipelineAggregationBuilders) {
        this.pipelineAggregations = pipelineAggregationBuilders;
    }

    public void setIndicesBoost(List<IndexBoost> indicesBoost) {
        this.indicesBoost = indicesBoost;
    }

    @Nullable
    public SearchTemplateRequestBuilder getSearchTemplate() {
        return searchTemplate;
    }

    public void setSearchTemplate(@Nullable SearchTemplateRequestBuilder searchTemplate) {
        this.searchTemplate = searchTemplate;
    }

    public void setSuggestBuilder(SuggestBuilder suggestBuilder) {
        this.suggestBuilder = suggestBuilder;
    }

    @Nullable
    public SuggestBuilder getSuggestBuilder() {
        return suggestBuilder;
    }

    public void setSearchExtBuilders(List<SearchExtBuilder> searchExtBuilders) {
        this.searchExtBuilders = searchExtBuilders;
    }

    public void addSearchExtBuilder(SearchExtBuilder searchExtBuilder) {
        if (searchExtBuilders == null) {
            searchExtBuilders = new ArrayList<>();
        }
        searchExtBuilders.add(searchExtBuilder);
    }

    @Nullable
    public List<SearchExtBuilder> getSearchExtBuilders() {
        return searchExtBuilders;
    }
}
