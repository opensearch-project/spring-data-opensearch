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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.client.indices.PutIndexTemplateRequest;
import org.opensearch.common.lucene.search.function.CombineFunction;
import org.opensearch.common.lucene.search.function.FunctionScoreQuery;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.opensearch.index.query.functionscore.FunctionScoreQueryBuilder.FilterFunctionBuilder;
import org.opensearch.index.query.functionscore.GaussDecayFunctionBuilder;
import org.opensearch.index.reindex.DeleteByQueryRequest;
import org.opensearch.search.fetch.subphase.FetchSourceContext;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.core.query.RescorerQuery.ScoreMode;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.Remote;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

class RequestFactoryTests {

    @Nullable
    private static RequestFactory requestFactory;

    @Nullable
    private static MappingElasticsearchConverter converter;

    @BeforeAll
    static void setUpAll() {
        SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
        mappingContext.setInitialEntitySet(
                new HashSet<>(Arrays.asList(Person.class, EntityWithSeqNoPrimaryTerm.class)));
        mappingContext.afterPropertiesSet();

        converter = new MappingElasticsearchConverter(mappingContext, new GenericConversionService());
        converter.afterPropertiesSet();

        requestFactory = new RequestFactory((converter));
    }

    @Test // DATAES-187
    public void shouldUsePageableOffsetToSetFromInSearchRequest() {

        // given
        Pageable pageable = new PageRequest(1, 10, Sort.unsorted()) {
            @Override
            public long getOffset() {
                return 30;
            }
        };

        NativeSearchQuery query = new NativeSearchQueryBuilder() //
                .withPageable(pageable) //
                .build();

        // when
        SearchRequest searchRequest = requestFactory.searchRequest(query, null, null, IndexCoordinates.of("test"));

        // then
        assertThat(searchRequest.source().from()).isEqualTo(30);
    }

    @Test // DATAES-227
    public void shouldUseUpsertOnUpdate() {

        // given
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", "1");
        doc.put("message", "test");

        org.springframework.data.elasticsearch.core.document.Document document =
                org.springframework.data.elasticsearch.core.document.Document.from(doc);

        UpdateQuery updateQuery = UpdateQuery.builder("1") //
                .withDocument(document) //
                .withUpsert(document) //
                .build();

        // when
        UpdateRequest request = requestFactory.updateRequest(updateQuery, IndexCoordinates.of("index"));

        // then
        assertThat(request).isNotNull();
        assertThat(request.upsertRequest()).isNotNull();
    }

    @Test // DATAES-693
    public void shouldReturnSourceWhenRequested() {
        // given
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", "1");
        doc.put("message", "test");

        org.springframework.data.elasticsearch.core.document.Document document =
                org.springframework.data.elasticsearch.core.document.Document.from(doc);

        UpdateQuery updateQuery = UpdateQuery.builder("1") //
                .withDocument(document) //
                .withFetchSource(true) //
                .build();

        // when
        UpdateRequest request = requestFactory.updateRequest(updateQuery, IndexCoordinates.of("index"));

        // then
        assertThat(request).isNotNull();
        assertThat(request.fetchSource()).isEqualTo(FetchSourceContext.FETCH_SOURCE);
    }

    @Test
    // DATAES-734
    void shouldBuildSearchWithGeoSortSort() throws JSONException {
        CriteriaQuery query = new CriteriaQuery(new Criteria("lastName").is("Smith"));
        Sort sort = Sort.by(new GeoDistanceOrder("location", new GeoPoint(49.0, 8.4)));
        query.addSort(sort);

        converter.updateQuery(query, Person.class);

        String expected = '{' + //
                "  \"query\": {"
                + //
                "    \"bool\": {"
                + //
                "      \"must\": ["
                + //
                "        {"
                + //
                "          \"query_string\": {"
                + //
                "            \"query\": \"Smith\","
                + //
                "            \"fields\": ["
                + //
                "              \"last-name^1.0\""
                + //
                "            ]"
                + //
                "          }"
                + //
                "        }"
                + //
                "      ]"
                + //
                "    }"
                + //
                "  },"
                + //
                "  \"sort\": ["
                + //
                "    {"
                + //
                "      \"_geo_distance\": {"
                + //
                "        \"current-location\": ["
                + //
                "          {"
                + //
                "            \"lat\": 49.0,"
                + //
                "            \"lon\": 8.4"
                + //
                "          }"
                + //
                "        ],"
                + //
                "        \"unit\": \"m\","
                + //
                "        \"distance_type\": \"arc\","
                + //
                "        \"order\": \"asc\","
                + //
                "        \"mode\": \"min\","
                + //
                "        \"ignore_unmapped\": false"
                + //
                "      }"
                + //
                "    }"
                + //
                "  ]"
                + //
                '}';

        String searchRequest = requestFactory
                .searchRequest(query, null, Person.class, IndexCoordinates.of("persons"))
                .source()
                .toString();

        assertEquals(expected, searchRequest, false);
    }

    @Test
    // DATAES-449
    void shouldAddRouting() {
        String route = "route66";
        CriteriaQuery query = new CriteriaQuery(new Criteria("lastName").is("Smith"));
        query.setRoute(route);
        converter.updateQuery(query, Person.class);

        SearchRequest searchRequest =
                requestFactory.searchRequest(query, null, Person.class, IndexCoordinates.of("persons"));

        assertThat(searchRequest.routing()).isEqualTo(route);
    }

    @Test
    // DATAES-765
    void shouldAddMaxQueryWindowForUnpagedToRequest() {
        Query query = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withPageable(Pageable.unpaged())
                .build();

        SearchRequest searchRequest =
                requestFactory.searchRequest(query, null, Person.class, IndexCoordinates.of("persons"));

        assertThat(searchRequest.source().from()).isEqualTo(0);
        assertThat(searchRequest.source().size()).isEqualTo(RequestFactory.INDEX_MAX_RESULT_WINDOW);
    }

    @Test
    // DATAES-799
    void shouldIncludeSeqNoAndPrimaryTermFromIndexQueryToIndexRequest() {
        IndexQuery query = new IndexQuery();
        query.setObject(new Person());
        query.setSeqNo(1L);
        query.setPrimaryTerm(2L);

        IndexRequest request = requestFactory.indexRequest(query, IndexCoordinates.of("persons"));

        assertThat(request.ifSeqNo()).isEqualTo(1L);
        assertThat(request.ifPrimaryTerm()).isEqualTo(2L);
    }

    @Test
    // DATAES-799
    void shouldNotRequestSeqNoAndPrimaryTermWhenEntityClassDoesNotContainSeqNoPrimaryTermProperty() {
        Query query = new NativeSearchQueryBuilder().build();

        SearchRequest request = requestFactory.searchRequest(query, null, Person.class, IndexCoordinates.of("persons"));

        assertThat(request.source().seqNoAndPrimaryTerm()).isNull();
    }

    @Test
    // DATAES-799
    void shouldRequestSeqNoAndPrimaryTermWhenEntityClassContainsSeqNoPrimaryTermProperty() {
        Query query = new NativeSearchQueryBuilder().build();

        SearchRequest request = requestFactory.searchRequest(
                query, null, EntityWithSeqNoPrimaryTerm.class, IndexCoordinates.of("seqNoPrimaryTerm"));

        assertThat(request.source().seqNoAndPrimaryTerm()).isTrue();
    }

    @Test
    // DATAES-799
    void shouldNotRequestSeqNoAndPrimaryTermWhenEntityClassIsNull() {
        Query query = new NativeSearchQueryBuilder().build();

        SearchRequest request = requestFactory.searchRequest(query, null, null, IndexCoordinates.of("persons"));

        assertThat(request.source().seqNoAndPrimaryTerm()).isNull();
    }

    @Test
    // DATAES-864
    void shouldBuildIndicesAliasRequest() throws IOException, JSONException {

        AliasActions aliasActions = new AliasActions();

        aliasActions.add(new AliasAction.Add(AliasActionParameters.builder()
                .withIndices("index1", "index2")
                .withAliases("alias1")
                .build()));
        aliasActions.add(new AliasAction.Remove(AliasActionParameters.builder()
                .withIndices("index3")
                .withAliases("alias1")
                .build()));

        aliasActions.add(new AliasAction.RemoveIndex(
                AliasActionParameters.builder().withIndices("index3").build()));

        aliasActions.add(new AliasAction.Add(AliasActionParameters.builder()
                .withIndices("index4")
                .withAliases("alias4")
                .withRouting("routing")
                .withIndexRouting("indexRouting")
                .withSearchRouting("searchRouting")
                .withIsHidden(true)
                .withIsWriteIndex(true)
                .build()));

        Query query = new CriteriaQuery(new Criteria("lastName").is("Smith"));
        aliasActions.add(new AliasAction.Add(AliasActionParameters.builder()
                .withIndices("index5")
                .withAliases("alias5")
                .withFilterQuery(query, Person.class)
                .build()));

        String expected = "{\n" + //
                "  \"actions\": [\n"
                + //
                "    {\n"
                + //
                "      \"add\": {\n"
                + //
                "        \"indices\": [\n"
                + //
                "          \"index1\",\n"
                + //
                "          \"index2\"\n"
                + //
                "        ],\n"
                + //
                "        \"aliases\": [\n"
                + //
                "          \"alias1\"\n"
                + //
                "        ]\n"
                + //
                "      }\n"
                + //
                "    },\n"
                + //
                "    {\n"
                + //
                "      \"remove\": {\n"
                + //
                "        \"indices\": [\n"
                + //
                "          \"index3\"\n"
                + //
                "        ],\n"
                + //
                "        \"aliases\": [\n"
                + //
                "          \"alias1\"\n"
                + //
                "        ]\n"
                + //
                "      }\n"
                + //
                "    },\n"
                + //
                "    {\n"
                + //
                "      \"remove_index\": {\n"
                + //
                "        \"indices\": [\n"
                + //
                "          \"index3\"\n"
                + //
                "        ]\n"
                + //
                "      }\n"
                + //
                "    },\n"
                + //
                "    {\n"
                + //
                "      \"add\": {\n"
                + //
                "        \"indices\": [\n"
                + //
                "          \"index4\"\n"
                + //
                "        ],\n"
                + //
                "        \"aliases\": [\n"
                + //
                "          \"alias4\"\n"
                + //
                "        ],\n"
                + //
                "        \"routing\": \"routing\",\n"
                + //
                "        \"index_routing\": \"indexRouting\",\n"
                + //
                "        \"search_routing\": \"searchRouting\",\n"
                + //
                "        \"is_write_index\": true,\n"
                + //
                "        \"is_hidden\": true\n"
                + //
                "      }\n"
                + //
                "    },\n"
                + //
                "    {\n"
                + //
                "      \"add\": {\n"
                + //
                "        \"indices\": [\n"
                + //
                "          \"index5\"\n"
                + //
                "        ],\n"
                + //
                "        \"aliases\": [\n"
                + //
                "          \"alias5\"\n"
                + //
                "        ],\n"
                + //
                "        \"filter\": {\n"
                + //
                "          \"bool\": {\n"
                + //
                "            \"must\": [\n"
                + //
                "              {\n"
                + //
                "                \"query_string\": {\n"
                + //
                "                  \"query\": \"Smith\",\n"
                + //
                "                  \"fields\": [\n"
                + //
                "                    \"last-name^1.0\"\n"
                + //
                "                  ],\n"
                + //
                "                  \"type\": \"best_fields\",\n"
                + //
                "                  \"default_operator\": \"and\",\n"
                + //
                "                  \"max_determinized_states\": 10000,\n"
                + //
                "                  \"enable_position_increments\": true,\n"
                + //
                "                  \"fuzziness\": \"AUTO\",\n"
                + //
                "                  \"fuzzy_prefix_length\": 0,\n"
                + //
                "                  \"fuzzy_max_expansions\": 50,\n"
                + //
                "                  \"phrase_slop\": 0,\n"
                + //
                "                  \"escape\": false,\n"
                + //
                "                  \"auto_generate_synonyms_phrase_query\": true,\n"
                + //
                "                  \"fuzzy_transpositions\": true,\n"
                + //
                "                  \"boost\": 1.0\n"
                + //
                "                }\n"
                + //
                "              }\n"
                + //
                "            ],\n"
                + //
                "            \"adjust_pure_negative\": true,\n"
                + //
                "            \"boost\": 1.0\n"
                + //
                "          }\n"
                + //
                "        }\n"
                + //
                "      }\n"
                + //
                "    }\n"
                + //
                "  ]\n"
                + //
                "}"; //

        IndicesAliasesRequest indicesAliasesRequest = requestFactory.indicesAliasesRequest(aliasActions);

        String json = requestToString(indicesAliasesRequest);

        assertEquals(expected, json, false);
    }

    @Test
    void shouldCreatePutIndexTemplateRequest() throws JSONException, IOException {
        var esSettingsDocument = org.springframework.data.elasticsearch.core.document.Document.create();
        esSettingsDocument.put("index.number_of_replicas", 2);
        esSettingsDocument.put("index.number_of_shards", 3);
        esSettingsDocument.put("index.refresh_interval", "7s");
        esSettingsDocument.put("index.store.type", "oops");
        var mappings = org.springframework.data.elasticsearch.core.document.Document.parse(
                "{\"properties\":{\"price\":{\"type\":\"double\"}}}");
        AliasActions aliasActions = new AliasActions(
                new AliasAction.Add(AliasActionParameters.builderForTemplate()
                        .withAliases("alias1", "alias2")
                        .build()),
                new AliasAction.Add(AliasActionParameters.builderForTemplate()
                        .withAliases("alias3")
                        .withRouting("11")
                        .build()));
        Settings settings = new Settings();
        settings.put("index.number_of_replicas", 2);
        settings.put("index.number_of_shards", 3);
        settings.put("index.refresh_interval", "7s");
        settings.put("index.store.type", "oops");
        var request = org.springframework.data.elasticsearch.core.index.PutIndexTemplateRequest.builder()
                .withName("test-template")
                .withIndexPatterns("test-*")
                .withSettings(settings)
                .withMapping(mappings) //
                .withAliasActions(aliasActions) //
                .build();

        PutIndexTemplateRequest actualPutIndexTemplateRequest = requestFactory.putIndexTemplateRequest(request);

        String actualRequestJson = requestToString(actualPutIndexTemplateRequest);
        String expectedRequestJson = StreamUtils.copyToString(
                new ClassPathResource("index-template-requests/put-index-template.json").getInputStream(),
                StandardCharsets.UTF_8);
        assertEquals(expectedRequestJson, actualRequestJson, false);
    }

    @Test // DATAES-247
    @DisplayName("should set op_type INDEX if not specified")
    void shouldSetOpTypeIndexIfNotSpecifiedAndIdIsSet() {

        IndexQuery indexQuery = new IndexQueryBuilder()
                .withId("42")
                .withObject(new Person("42", "Smith"))
                .build();

        IndexRequest indexRequest = requestFactory.indexRequest(indexQuery, IndexCoordinates.of("optype"));

        assertThat(indexRequest.opType()).isEqualTo(DocWriteRequest.OpType.INDEX);
    }

    @Test // DATAES-247
    @DisplayName("should set op_type CREATE if specified")
    void shouldSetOpTypeCreateIfSpecified() {

        IndexQuery indexQuery = new IndexQueryBuilder()
                .withOpType(IndexQuery.OpType.CREATE)
                .withId("42")
                .withObject(new Person("42", "Smith"))
                .build();

        IndexRequest indexRequest = requestFactory.indexRequest(indexQuery, IndexCoordinates.of("optype"));

        assertThat(indexRequest.opType()).isEqualTo(DocWriteRequest.OpType.CREATE);
    }

    @Test // DATAES-247
    @DisplayName("should set op_type INDEX if specified")
    void shouldSetOpTypeIndexIfSpecified() {

        IndexQuery indexQuery = new IndexQueryBuilder()
                .withOpType(IndexQuery.OpType.INDEX)
                .withId("42")
                .withObject(new Person("42", "Smith"))
                .build();

        IndexRequest indexRequest = requestFactory.indexRequest(indexQuery, IndexCoordinates.of("optype"));

        assertThat(indexRequest.opType()).isEqualTo(DocWriteRequest.OpType.INDEX);
    }

    @Test // DATAES-1003
    @DisplayName("should set timeout to request")
    void shouldSetTimeoutToRequest() {
        Query query = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withTimeout(Duration.ofSeconds(1))
                .build();

        SearchRequest searchRequest =
                requestFactory.searchRequest(query, null, Person.class, IndexCoordinates.of("persons"));

        assertThat(searchRequest.source().timeout().getMillis())
                .isEqualTo(Duration.ofSeconds(1).toMillis());
    }

    private String requestToString(ToXContent request) throws IOException {
        return XContentHelper.toXContent(request, XContentType.JSON, true).utf8ToString();
    }

    @Test
    // #1686
    void shouldBuildSearchWithRescorerQuery() throws JSONException {
        CriteriaQuery query = new CriteriaQuery(new Criteria("lastName").is("Smith"));
        RescorerQuery rescorerQuery = new RescorerQuery(
                        new NativeSearchQueryBuilder() //
                                .withQuery(QueryBuilders.functionScoreQuery(
                                                new FunctionScoreQueryBuilder.FilterFunctionBuilder[] {
                                                    new FilterFunctionBuilder(
                                                            QueryBuilders.existsQuery("someField"),
                                                            new GaussDecayFunctionBuilder(
                                                                            "someField", 0, 100000.0, null, 0.683)
                                                                    .setWeight(5.022317f)),
                                                    new FilterFunctionBuilder(
                                                            QueryBuilders.existsQuery("anotherField"),
                                                            new GaussDecayFunctionBuilder(
                                                                            "anotherField",
                                                                            "202102",
                                                                            "31536000s",
                                                                            null,
                                                                            0.683)
                                                                    .setWeight(4.170836f))
                                                })
                                        .scoreMode(FunctionScoreQuery.ScoreMode.SUM)
                                        .maxBoost(50.0f)
                                        .boostMode(CombineFunction.AVG)
                                        .boost(1.5f))
                                .build())
                .withWindowSize(50)
                .withQueryWeight(2.0f)
                .withRescoreQueryWeight(5.0f)
                .withScoreMode(ScoreMode.Multiply);

        RescorerQuery anotherRescorerQuery = new RescorerQuery(
                        new NativeSearchQueryBuilder() //
                                .withQuery(QueryBuilders.matchPhraseQuery("message", "the quick brown")
                                        .slop(2))
                                .build())
                .withWindowSize(100)
                .withQueryWeight(0.7f)
                .withRescoreQueryWeight(1.2f);

        query.addRescorerQuery(rescorerQuery);
        query.addRescorerQuery(anotherRescorerQuery);

        converter.updateQuery(query, Person.class);

        String expected = '{' + //
                "  \"query\": {"
                + //
                "    \"bool\": {"
                + //
                "      \"must\": ["
                + //
                "        {"
                + //
                "          \"query_string\": {"
                + //
                "            \"query\": \"Smith\","
                + //
                "            \"fields\": ["
                + //
                "              \"last-name^1.0\""
                + //
                "            ]"
                + //
                "          }"
                + //
                "        }"
                + //
                "      ]"
                + //
                "    }"
                + //
                "  },"
                + //
                "  \"rescore\": [{\n"
                + "      \"window_size\" : 100,\n" + "      \"query\" : {\n"
                + "         \"rescore_query\" : {\n" + "            \"match_phrase\" : {\n"
                + "               \"message\" : {\n"
                + "                  \"query\" : \"the quick brown\",\n" + "                  \"slop\" : 2\n"
                + "               }\n" + "            }\n" + "         },\n" + "         \"query_weight\" : 0.7,\n"
                + "         \"rescore_query_weight\" : 1.2\n" + "      }\n" + "   }," + "  {\n"
                + "     \"window_size\": 50,\n"
                + "     \"query\": {\n" + "      				\"rescore_query\": {\n" + "      							\"function_score\": {\n"
                + "                        \"query\": {\n" + "                            \"match_all\": {\n"
                + "                                \"boost\": 1.0\n" + "                            }\n"
                + "                        },\n" + "                        \"functions\": [\n"
                + "                            {\n" + "                                \"filter\": {\n"
                + "                                    \"exists\": {\n"
                + "                                        \"field\": \"someField\",\n"
                + "                                        \"boost\": 1.0\n" + "                                    }\n"
                + "                                },\n" + "                                \"weight\": 5.022317,\n"
                + "                                \"gauss\": {\n"
                + "                                    \"someField\": {\n"
                + "                                        \"origin\": 0.0,\n"
                + "                                        \"scale\": 100000.0,\n"
                + "                                        \"decay\": 0.683\n"
                + "                                    },\n"
                + "                                    \"multi_value_mode\": \"MIN\"\n"
                + "                                }\n"
                + "                            },\n" + "                            {\n"
                + "                                \"filter\": {\n"
                + "                                    \"exists\": {\n"
                + "                                        \"field\": \"anotherField\",\n"
                + "                                        \"boost\": 1.0\n" + "                                    }\n"
                + "                                },\n" + "                                \"weight\": 4.170836,\n"
                + "                                \"gauss\": {\n"
                + "                                    \"anotherField\": {\n"
                + "                                        \"origin\": \"202102\",\n"
                + "                                        \"scale\": \"31536000s\",\n"
                + "                                        \"decay\": 0.683\n"
                + "                                    },\n"
                + "                                    \"multi_value_mode\": \"MIN\"\n"
                + "                                }\n"
                + "                            }\n" + "                        ],\n"
                + "                        \"score_mode\": \"sum\",\n"
                + "                        \"boost_mode\": \"avg\",\n"
                + "                        \"max_boost\": 50.0,\n" + "                        \"boost\": 1.5\n"
                + "                    }\n" + "             },\n" + "      \"query_weight\": 2.0,"
                + "      \"rescore_query_weight\": 5.0," + "      \"score_mode\": \"multiply\"" + "   }\n" + " }\n"
                + " ]\n"
                + '}';

        String searchRequest = requestFactory
                .searchRequest(query, null, Person.class, IndexCoordinates.of("persons"))
                .source()
                .toString();

        assertEquals(expected, searchRequest, false);
    }

    @Test // #1564
    @DisplayName("should not set request_cache on default SearchRequest")
    void shouldNotSetRequestCacheOnDefaultSearchRequest() {

        Query query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();

        SearchRequest searchRequest =
                requestFactory.searchRequest(query, null, Person.class, IndexCoordinates.of("persons"));

        assertThat(searchRequest.requestCache()).isNull();
    }

    @Test // #1564
    @DisplayName("should set request_cache true on SearchRequest")
    void shouldSetRequestCacheTrueOnSearchRequest() {

        Query query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
        query.setRequestCache(true);

        SearchRequest searchRequest =
                requestFactory.searchRequest(query, null, Person.class, IndexCoordinates.of("persons"));

        assertThat(searchRequest.requestCache()).isTrue();
    }

    @Test // #1564
    @DisplayName("should set request_cache false on SearchRequest")
    void shouldSetRequestCacheFalseOnSearchRequest() {

        Query query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
        query.setRequestCache(false);

        SearchRequest searchRequest =
                requestFactory.searchRequest(query, null, Person.class, IndexCoordinates.of("persons"));

        assertThat(searchRequest.requestCache()).isFalse();
    }

    @Test // #2004
    @DisplayName("should set stored fields on SearchRequest")
    void shouldSetStoredFieldsOnSearchRequest() {

        Query query = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery())
                .withStoredFields("lastName", "location")
                .build();

        SearchRequest searchRequest =
                requestFactory.searchRequest(query, null, Person.class, IndexCoordinates.of("persons"));

        assertThat(searchRequest.source().storedFields()).isNotNull();
        assertThat(searchRequest.source().storedFields().fieldNames())
                .isEqualTo(Arrays.asList("last-name", "current-location"));
    }

    @Test
    // #1529
    void shouldCreateReindexRequest() throws IOException, JSONException {
        final String expected = "{\n" + //
                "    \"source\":{\n"
                + //
                "        \"remote\":{\n"
                + //
                "                \"username\":\"admin\",\n"
                + //
                "                \"password\":\"password\",\n"
                + //
                "                \"host\":\"http://localhost:9200/elasticsearch\",\n"
                + //
                "                \"socket_timeout\":\"30s\",\n"
                + //
                "                \"connect_timeout\":\"30s\"\n"
                + //
                "            },\n"
                + //
                "        \"index\":[\"source_1\",\"source_2\"],\n"
                + //
                "        \"size\":5,\n"
                + //
                "        \"query\":{\"match_all\":{}},\n"
                + //
                "        \"_source\":{\"includes\":[\"name\"],\"excludes\":[]},\n"
                + //
                "        \"slice\":{\"id\":1,\"max\":20}\n"
                + //
                "    },\n"
                + //
                "    \"dest\":{\n"
                + //
                "        \"index\":\"destination\",\n"
                + //
                "        \"routing\":\"routing\",\n"
                + //
                "        \"op_type\":\"create\",\n"
                + //
                "        \"pipeline\":\"pipeline\",\n"
                + //
                "        \"version_type\":\"external\"\n"
                + //
                "    },\n"
                + //
                "    \"max_docs\":10,\n"
                + //
                "    \"script\":{\"source\":\"Math.max(1,2)\",\"lang\":\"java\"},\n"
                + //
                "    \"conflicts\":\"proceed\"\n"
                + //
                "}";

        Remote remote = Remote.builder("http", "localhost", 9200)
                .withPathPrefix("elasticsearch")
                .withUsername("admin")
                .withPassword("password")
                .withConnectTimeout(Duration.ofSeconds(30))
                .withSocketTimeout(Duration.ofSeconds(30))
                .build();

        ReindexRequest reindexRequest = ReindexRequest.builder(
                        IndexCoordinates.of("source_1", "source_2"), IndexCoordinates.of("destination"))
                .withConflicts(ReindexRequest.Conflicts.PROCEED)
                .withMaxDocs(10L)
                .withSourceQuery(new NativeSearchQueryBuilder()
                        .withQuery(matchAllQuery())
                        .build())
                .withSourceSize(5)
                .withSourceSourceFilter(
                        new FetchSourceFilterBuilder().withIncludes("name").build())
                .withSourceRemote(remote)
                .withSourceSlice(1, 20)
                .withDestOpType(IndexQuery.OpType.CREATE)
                .withDestVersionType(Document.VersionType.EXTERNAL)
                .withDestPipeline("pipeline")
                .withDestRouting("routing")
                .withScript("Math.max(1,2)", "java")
                .build();

        final String json = requestToString(requestFactory.reindexRequest(reindexRequest));

        assertEquals(expected, json, false);
    }

    @Test
    // #1529
    void shouldAllowSourceQueryForReindexWithoutRemote() throws IOException, JSONException {
        final String expected = "{\n" + //
                "    \"source\":{\n"
                + //
                "        \"index\":[\"source\"],\n"
                + //
                "        \"query\":{\"match_all\":{}}\n"
                + //
                "    },\n"
                + //
                "    \"dest\":{\n"
                + //
                "        \"index\":\"destination\",\n"
                + //
                "        \"op_type\":\"index\",\n"
                + //
                "        \"version_type\":\"internal\"\n"
                + //
                "    }\n"
                + //
                "}";

        ReindexRequest reindexRequest = ReindexRequest.builder(
                        IndexCoordinates.of("source"), IndexCoordinates.of("destination"))
                .withSourceQuery(new NativeSearchQueryBuilder()
                        .withQuery(matchAllQuery())
                        .build())
                .build();

        final String json = requestToString(requestFactory.reindexRequest(reindexRequest));

        assertEquals(expected, json, false);
    }

    @Test // #2075
    @DisplayName("should not fail on empty Option set during toOpenSearchIndicesOptions")
    void shouldNotFailOnEmptyOptionsOnToElasticsearchIndicesOptions() {
        assertThat(requestFactory.toOpenSearchIndicesOptions(new IndicesOptions(
                        EnumSet.noneOf(IndicesOptions.Option.class), EnumSet.of(IndicesOptions.WildcardStates.OPEN))))
                .isNotNull();
    }

    @Test // #2075
    @DisplayName("should not fail on empty WildcardState set during toOpenSearchIndicesOptions")
    void shouldNotFailOnEmptyWildcardStatesOnToElasticsearchIndicesOptions() {
        assertThat(requestFactory.toOpenSearchIndicesOptions(
                        IndicesOptions.STRICT_SINGLE_INDEX_NO_EXPAND_FORBID_CLOSED))
                .isNotNull();
    }

    @Test // #2043
    @DisplayName("should use index name from query if set in bulk index")
    void shouldUseIndexNameFromQueryIfSetInBulkIndex() {

        String queryIndexName = "query-index-name";
        String methodIndexName = "method-index-name";
        IndexQuery indexQuery = new IndexQueryBuilder()
                .withIndex(queryIndexName)
                .withId("42")
                .withObject(new Person())
                .build();

        IndexRequest indexRequest = requestFactory.indexRequest(indexQuery, IndexCoordinates.of(methodIndexName));

        assertThat(indexRequest.index()).isEqualTo(queryIndexName);
    }

    @Test // #2043
    @DisplayName("should use index name from method if none is set in query in bulk index")
    void shouldUseIndexNameFromMethodIfNoneIsSetInQueryInBulkIndex() {

        String methodIndexName = "method-index-name";
        IndexQuery indexQuery =
                new IndexQueryBuilder().withId("42").withObject(new Person()).build();

        IndexRequest indexRequest = requestFactory.indexRequest(indexQuery, IndexCoordinates.of(methodIndexName));

        assertThat(indexRequest.index()).isEqualTo(methodIndexName);
    }

    @Test // #2043
    @DisplayName("should use index name from query if set in bulk update")
    void shouldUseIndexNameFromQueryIfSetInBulkUpdate() {

        String queryIndexName = "query-index-name";
        String methodIndexName = "method-index-name";
        UpdateQuery updateQuery = UpdateQuery.builder("42")
                .withIndex(queryIndexName)
                .withDocument(org.springframework.data.elasticsearch.core.document.Document.create())
                .build();

        UpdateRequest updateRequest = requestFactory.updateRequest(updateQuery, IndexCoordinates.of(methodIndexName));

        assertThat(updateRequest.index()).isEqualTo(queryIndexName);
    }

    @Test // #2043
    @DisplayName("should use index name from method if none is set in query in bulk update")
    void shouldUseIndexNameFromMethodIfNoneIsSetInQueryInBulkUpdate() {

        String methodIndexName = "method-index-name";
        UpdateQuery updateQuery = UpdateQuery.builder("42")
                .withDocument(org.springframework.data.elasticsearch.core.document.Document.create())
                .build();

        UpdateRequest updateRequest = requestFactory.updateRequest(updateQuery, IndexCoordinates.of(methodIndexName));

        assertThat(updateRequest.index()).isEqualTo(methodIndexName);
    }

    @Test
    void shouldSetRefreshTrueIfRefreshPolicyIsImmediateInDeleteByQuery() {
        String methodIndexName = "method-index-name";
        Query searchQuery = new NativeSearchQueryBuilder()
            .withQuery(matchAllQuery())
            .build();

        DeleteByQueryRequest deleteByQueryRequest = requestFactory.deleteByQueryRequest(searchQuery, null, Person.class,
                                                                                        IndexCoordinates.of(methodIndexName), RefreshPolicy.IMMEDIATE);

        assertThat(deleteByQueryRequest.isRefresh()).isTrue();
    }

    @Test
    void shouldSetRefreshFalseIfRefreshPolicyIsNoneInDeleteByQuery() {
        String methodIndexName = "method-index-name";
        Query searchQuery = new NativeSearchQueryBuilder()
            .withQuery(matchAllQuery())
            .build();

        DeleteByQueryRequest deleteByQueryRequest = requestFactory.deleteByQueryRequest(searchQuery, null, Person.class,
                                                                                        IndexCoordinates.of(methodIndexName), RefreshPolicy.NONE);

        assertThat(deleteByQueryRequest.isRefresh()).isFalse();
    }

    @Test
    void shouldSetRefreshFalseIfRefreshPolicyIsWaitUntilInDeleteByQuery() {
        String methodIndexName = "method-index-name";
        Query searchQuery = new NativeSearchQueryBuilder()
            .withQuery(matchAllQuery())
            .build();

        DeleteByQueryRequest deleteByQueryRequest = requestFactory.deleteByQueryRequest(searchQuery, null, Person.class,
                                                                                        IndexCoordinates.of(methodIndexName), RefreshPolicy.WAIT_UNTIL);

        assertThat(deleteByQueryRequest.isRefresh()).isFalse();
    }

    @Test // #335
    @DisplayName("should set refresh based on deleteRequest")
    void refreshSetByDeleteRequest() {
        var methodIndexName = "method-index-name";
        var query = new CriteriaQuery(new Criteria("lastName").contains("test"));
        var deleteQuery = DeleteQuery.builder(query).withRefresh(true).build();

        var deleteByQueryRequest = requestFactory.documentDeleteByQueryRequest(deleteQuery, null, Person.class,
            IndexCoordinates.of(methodIndexName), null);

        assertThat(deleteByQueryRequest.isRefresh()).isTrue();
    }

    // region entities
    static class Person {
        @Nullable
        @Id
        String id;

        @Nullable
        @Field(name = "last-name")
        String lastName;

        @Nullable
        @Field(name = "current-location")
        GeoPoint location;

        public Person() {}

        public Person(@Nullable String id, @Nullable String lastName) {
            this.id = id;
            this.lastName = lastName;
        }

        public Person(@Nullable String id, @Nullable String lastName, @Nullable GeoPoint location) {
            this.id = id;
            this.lastName = lastName;
            this.location = location;
        }

        @Nullable
        public String getId() {
            return id;
        }

        public void setId(@Nullable String id) {
            this.id = id;
        }

        @Nullable
        public String getLastName() {
            return lastName;
        }

        public void setLastName(@Nullable String lastName) {
            this.lastName = lastName;
        }

        @Nullable
        public GeoPoint getLocation() {
            return location;
        }

        public void setLocation(@Nullable GeoPoint location) {
            this.location = location;
        }
    }

    static class EntityWithSeqNoPrimaryTerm {
        @Nullable
        private SeqNoPrimaryTerm seqNoPrimaryTerm;
    }
    // endregion
}
