/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.msearch.RequestItem;
import org.opensearch.client.opensearch.core.search.TrackHits;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.DocValueField;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 */
class RequestConverterTest {

    private static final SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
    private static final MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
    private JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper();
    private RequestConverter requestConverter = new RequestConverter(converter, jsonpMapper);

    @Test // #2316
    @DisplayName("should add docvalue_fields")
    void shouldAddDocvalueFields() {

        var docValueFields = List.of( //
            new DocValueField("field1"), //
            new DocValueField("field2", "format2") //
        );
        // doesn't matter what type of query is used, the relevant part for docvalue_fields is in the base builder.
        var query = StringQuery.builder("""
                {
                    "match_all":{}
                }
                """) //
            .withDocValueFields(docValueFields) //
            .build();

        var searchRequest = requestConverter.searchRequest(query,null, SampleEntity.class, IndexCoordinates.of("foo"), true);

        var fieldAndFormats = searchRequest.docvalueFields();
        assertThat(fieldAndFormats).hasSize(2);
        assertThat(fieldAndFormats.get(0).field()).isEqualTo("field1");
        assertThat(fieldAndFormats.get(0).format()).isNull();
        assertThat(fieldAndFormats.get(1).field()).isEqualTo("field2");
        assertThat(fieldAndFormats.get(1).format()).isEqualTo("format2");
    }

    @Test // #335
    @DisplayName("should set refresh based on deleteRequest")
    void refreshSetByDeleteRequest() {
        var query = new CriteriaQuery(new Criteria("text").contains("test"));
        var deleteQuery = DeleteQuery.builder(query).withRefresh(true).build();

        var deleteByQueryRequest = requestConverter.documentDeleteByQueryRequest(deleteQuery, null, SampleEntity.class,
            IndexCoordinates.of("foo"),
            null);

        assertThat(deleteByQueryRequest.refresh()).isEqualTo(Refresh.True);
    }


    @Test // #542
    @DisplayName("should set track_total_hits to true on searchMsearchRequest")
    void shouldSetTrackTotalTrueOnMultiSearch() {
        var query = new NativeQueryBuilder()
                .withQuery(Queries.matchAllQuery().toQuery())
                .withTrackTotalHits(true)
                .build();

        var multiSearchQueryParameters = new ArrayList<OpenSearchTemplate.MultiSearchQueryParameter>();
        multiSearchQueryParameters.add(new OpenSearchTemplate.MultiSearchQueryParameter(query, SampleEntity.class, IndexCoordinates.of("foo")));

        var searchRequest = requestConverter.searchMsearchRequest(multiSearchQueryParameters, null);

        List<RequestItem> searches = searchRequest.searches();
        assertThat(searches).hasSize(1);

        TrackHits trackTotalHits = searches.getFirst().body().trackTotalHits();
        assertThat(trackTotalHits).isNotNull();
        assertThat(trackTotalHits.isCount()).isFalse();
        assertThat(trackTotalHits.isEnabled()).isTrue();
        assertThat(trackTotalHits.enabled()).isTrue();
    }

    @Test // #542
    @DisplayName("should set track_total_hits to false on searchMsearchRequest")
    void shouldSetTrackTotalFalseOnMultiSearch() {
        var query = new NativeQueryBuilder()
                .withQuery(Queries.matchAllQuery().toQuery())
                .withTrackTotalHits(false)
                .build();

        var multiSearchQueryParameters = new ArrayList<OpenSearchTemplate.MultiSearchQueryParameter>();
        multiSearchQueryParameters.add(new OpenSearchTemplate.MultiSearchQueryParameter(query, SampleEntity.class, IndexCoordinates.of("foo")));

        var searchRequest = requestConverter.searchMsearchRequest(multiSearchQueryParameters, null);

        List<RequestItem> searches = searchRequest.searches();
        assertThat(searches).hasSize(1);

        TrackHits trackTotalHits = searches.getFirst().body().trackTotalHits();
        assertThat(trackTotalHits).isNotNull();
        assertThat(trackTotalHits.isCount()).isFalse();
        assertThat(trackTotalHits.isEnabled()).isTrue();
        assertThat(trackTotalHits.enabled()).isFalse();
    }

    @Test // #542
    @DisplayName("should set track_total_hits to count value on searchMsearchRequest")
    void shouldSetTrackTotalCountValueOnMultiSearch() {
        int countValue = 5000;
        var query = new NativeQueryBuilder()
                .withQuery(Queries.matchAllQuery().toQuery())
                .withTrackTotalHitsUpTo(countValue)
                .build();

        var multiSearchQueryParameters = new ArrayList<OpenSearchTemplate.MultiSearchQueryParameter>();
        multiSearchQueryParameters.add(new OpenSearchTemplate.MultiSearchQueryParameter(query, SampleEntity.class, IndexCoordinates.of("foo")));

        var searchRequest = requestConverter.searchMsearchRequest(multiSearchQueryParameters, null);

        List<RequestItem> searches = searchRequest.searches();
        assertThat(searches).hasSize(1);

        TrackHits trackTotalHits = searches.getFirst().body().trackTotalHits();
        assertThat(trackTotalHits).isNotNull();
        assertThat(trackTotalHits.isEnabled()).isFalse();
        assertThat(trackTotalHits.isCount()).isTrue();
        assertThat(trackTotalHits.count()).isEqualTo(countValue);
    }

    @Document(indexName = "does-not-matter")
    static class SampleEntity {
        @Nullable
        @Id private String id;
        @Nullable
        @Field(type = FieldType.Text) private String text;
    }
}
