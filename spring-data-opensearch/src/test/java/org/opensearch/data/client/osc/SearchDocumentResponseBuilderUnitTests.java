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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch._types.ShardFailure;
import org.opensearch.client.opensearch._types.ShardStatistics;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.Suggest;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.springframework.data.elasticsearch.ElasticsearchErrorCause;
import org.springframework.data.elasticsearch.core.SearchShardStatistics;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;

/**
 * Tests for the factory class to create {@link SearchDocumentResponse} instances.
 *
 * @author Sébastien Comeau
 * @since 5.2
 */
class SearchDocumentResponseBuilderUnitTests {

    private JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper();

    @Test // #2681
    void shouldGetPhraseSuggestion() throws JSONException {
        // arrange
        final var hitsMetadata = new HitsMetadata.Builder<EntityAsMap>()
                .total(total -> total
                        .value(0)
                        .relation(TotalHitsRelation.Eq))
                .hits(new ArrayList<>())
                .build();

        final var suggestionTest = new Suggest.Builder<EntityAsMap>()
                .phrase(phrase -> phrase
                        .text("National")
                        .offset(0)
                        .length(8)
                        .options(option -> option
                                .text("nations")
                                .highlighted("highlighted-nations")
                                .score(0.11480146)
                                .collateMatch(false))
                        .options(option -> option
                                .text("national")
                                .highlighted("highlighted-national")
                                .score(0.08063514)
                                .collateMatch(false)))
                .build();

        final var sortProperties = ImmutableMap.<String, List<Suggest<EntityAsMap>>> builder()
                .put("suggestionTest", ImmutableList.of(suggestionTest))
                .build();

        // act
        final var actual = SearchDocumentResponseBuilder.from(hitsMetadata, null, null, null, null, sortProperties, null,
                jsonpMapper);

        // assert
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(actual).isNotNull();
        softly.assertThat(actual.getSuggest()).isNotNull();
        softly.assertThat(actual.getSuggest().getSuggestions()).isNotNull().hasSize(1);

        final var actualSuggestion = actual.getSuggest().getSuggestions().get(0);
        softly.assertThat(actualSuggestion.getName()).isEqualTo("suggestionTest");
        softly.assertThat(actualSuggestion.getEntries()).isNotNull().hasSize(1);

        final var actualEntry = actualSuggestion.getEntries().get(0);
        softly.assertThat(actualEntry).isNotNull();
        softly.assertThat(actualEntry.getText()).isEqualTo("National");
        softly.assertThat(actualEntry.getOffset()).isEqualTo(0);
        softly.assertThat(actualEntry.getLength()).isEqualTo(8);
        softly.assertThat(actualEntry.getOptions()).isNotNull().hasSize(2);

        final var actualOption1 = actualEntry.getOptions().get(0);
        softly.assertThat(actualOption1.getText()).isEqualTo("nations");
        softly.assertThat(actualOption1.getHighlighted()).isEqualTo("highlighted-nations");
        softly.assertThat(actualOption1.getScore()).isEqualTo(0.11480146);
        softly.assertThat(actualOption1.getCollateMatch()).isEqualTo(false);

        final var actualOption2 = actualEntry.getOptions().get(1);
        softly.assertThat(actualOption2.getText()).isEqualTo("national");
        softly.assertThat(actualOption2.getHighlighted()).isEqualTo("highlighted-national");
        softly.assertThat(actualOption2.getScore()).isEqualTo(0.08063514);
        softly.assertThat(actualOption2.getCollateMatch()).isEqualTo(false);

        softly.assertAll();
    }

    @Test // #2605
    void shouldGetShardStatisticsInfo() {
        // arrange
        HitsMetadata<EntityAsMap> hitsMetadata = new HitsMetadata.Builder<EntityAsMap>()
                .total(t -> t
                        .value(0)
                        .relation(TotalHitsRelation.Eq))
                .hits(new ArrayList<>())
                .build();

        ShardStatistics shards = new ShardStatistics.Builder()
                .total(15)
                .successful(14)
                .skipped(0)
                .failed(1)
                .failures(List.of(
                        ShardFailure.of(sfb -> sfb
                                .index("test-index")
                                .node("test-node")
                                .shard(1)
                                .reason(rb -> rb
                                        .reason("this is a mock failure in shards")
                                        .causedBy(cbb ->
                                                cbb.reason("inner reason")
                                                        .metadata(Map.of("hello", JsonData.of("world")))
                                                        .type("inner-reason-type")
                                        )
                                        .type("reason-type")

                                )
                                .status("fail")
                        )
                ))
                .build();

        // act
        SearchDocumentResponse response = SearchDocumentResponseBuilder.from(hitsMetadata, shards, null, null,
                null, null, null, jsonpMapper);

        // assert
        SearchShardStatistics shardStatistics = response.getSearchShardStatistics();
        assertThat(shardStatistics).isNotNull();
        assertThat(shardStatistics.getTotal()).isEqualTo(15);
        assertThat(shardStatistics.getSuccessful()).isEqualTo(14);
        assertThat(shardStatistics.getSkipped()).isEqualTo(0);
        assertThat(shardStatistics.getFailed()).isEqualTo(1);
        // assert failure
        List<SearchShardStatistics.Failure> failures = shardStatistics.getFailures();
        assertThat(failures.size()).isEqualTo(1);
        assertThat(failures).extracting(SearchShardStatistics.Failure::getIndex).containsExactly("test-index");
        assertThat(failures).extracting(SearchShardStatistics.Failure::getElasticsearchErrorCause)
                .extracting(ElasticsearchErrorCause::getReason)
                .containsExactly("this is a mock failure in shards");
        assertThat(failures).extracting(SearchShardStatistics.Failure::getElasticsearchErrorCause)
                .extracting(ElasticsearchErrorCause::getCausedBy)
                .extracting(ElasticsearchErrorCause::getReason)
                .containsExactly("inner reason");
    }
}
