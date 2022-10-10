/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.document;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.action.get.GetResponse;
import org.opensearch.common.bytes.BytesArray;
import org.opensearch.common.document.DocumentField;
import org.opensearch.data.client.orhlc.DocumentAdapters;
import org.opensearch.index.get.GetResult;
import org.opensearch.index.shard.ShardId;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchShardTarget;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.Explanation;
import org.springframework.data.elasticsearch.core.document.SearchDocument;

/**
 * Unit tests for {@link DocumentAdapters}.
 */
public class DocumentAdaptersUnitTests {

    @Test // DATAES-628, DATAES-848
    public void shouldAdaptGetResponse() {

        Map<String, DocumentField> fields =
                Collections.singletonMap("field", new DocumentField("field", Collections.singletonList("value")));

        GetResult getResult = new GetResult("index", "my-id", 1, 2, 42, true, null, fields, null);
        GetResponse response = new GetResponse(getResult);

        Document document = DocumentAdapters.from(response);

        assertThat(document.getIndex()).isEqualTo("index");
        assertThat(document.hasId()).isTrue();
        assertThat(document.getId()).isEqualTo("my-id");
        assertThat(document.hasVersion()).isTrue();
        assertThat(document.getVersion()).isEqualTo(42);
        assertThat(document.get("field")).isEqualTo("value");
        assertThat(document.hasSeqNo()).isTrue();
        assertThat(document.getSeqNo()).isEqualTo(1);
        assertThat(document.hasPrimaryTerm()).isTrue();
        assertThat(document.getPrimaryTerm()).isEqualTo(2);
    }

    @Test // DATAES-628, DATAES-848
    public void shouldAdaptGetResponseSource() {

        BytesArray source = new BytesArray("{\"field\":\"value\"}");

        GetResult getResult = new GetResult("index", "my-id", 1, 2, 42, true, source, Collections.emptyMap(), null);
        GetResponse response = new GetResponse(getResult);

        Document document = DocumentAdapters.from(response);

        assertThat(document.getIndex()).isEqualTo("index");
        assertThat(document.hasId()).isTrue();
        assertThat(document.getId()).isEqualTo("my-id");
        assertThat(document.hasVersion()).isTrue();
        assertThat(document.getVersion()).isEqualTo(42);
        assertThat(document.get("field")).isEqualTo("value");
        assertThat(document.hasSeqNo()).isTrue();
        assertThat(document.getSeqNo()).isEqualTo(1);
        assertThat(document.hasPrimaryTerm()).isTrue();
        assertThat(document.getPrimaryTerm()).isEqualTo(2);
    }

    @Test // DATAES-799, DATAES-848
    public void shouldAdaptGetResult() {

        Map<String, DocumentField> fields =
                Collections.singletonMap("field", new DocumentField("field", Collections.singletonList("value")));

        GetResult getResult = new GetResult("index", "my-id", 1, 2, 42, true, null, fields, null);

        Document document = DocumentAdapters.from(getResult);

        assertThat(document.getIndex()).isEqualTo("index");
        assertThat(document.hasId()).isTrue();
        assertThat(document.getId()).isEqualTo("my-id");
        assertThat(document.hasVersion()).isTrue();
        assertThat(document.getVersion()).isEqualTo(42);
        assertThat(document.get("field")).isEqualTo("value");
        assertThat(document.hasSeqNo()).isTrue();
        assertThat(document.getSeqNo()).isEqualTo(1);
        assertThat(document.hasPrimaryTerm()).isTrue();
        assertThat(document.getPrimaryTerm()).isEqualTo(2);
    }

    @Test // DATAES-799, DATAES-848
    public void shouldAdaptGetResultSource() {

        BytesArray source = new BytesArray("{\"field\":\"value\"}");

        GetResult getResult = new GetResult("index", "my-id", 1, 2, 42, true, source, Collections.emptyMap(), null);

        Document document = DocumentAdapters.from(getResult);

        assertThat(document.getIndex()).isEqualTo("index");
        assertThat(document.hasId()).isTrue();
        assertThat(document.getId()).isEqualTo("my-id");
        assertThat(document.hasVersion()).isTrue();
        assertThat(document.getVersion()).isEqualTo(42);
        assertThat(document.get("field")).isEqualTo("value");
        assertThat(document.hasSeqNo()).isTrue();
        assertThat(document.getSeqNo()).isEqualTo(1);
        assertThat(document.hasPrimaryTerm()).isTrue();
        assertThat(document.getPrimaryTerm()).isEqualTo(2);
    }

    @Test // DATAES-628, DATAES-848
    public void shouldAdaptSearchResponse() {

        Map<String, DocumentField> fields =
                Collections.singletonMap("field", new DocumentField("field", Collections.singletonList("value")));

        SearchShardTarget shard = new SearchShardTarget("node", new ShardId("index", "uuid", 42), null, null);
        SearchHit searchHit = new SearchHit(123, "my-id", null, fields);
        searchHit.shard(shard);
        searchHit.setSeqNo(1);
        searchHit.setPrimaryTerm(2);
        searchHit.score(42);

        SearchDocument document = DocumentAdapters.from(searchHit);

        assertThat(document.getIndex()).isEqualTo("index");
        assertThat(document.hasId()).isTrue();
        assertThat(document.getId()).isEqualTo("my-id");
        assertThat(document.hasVersion()).isFalse();
        assertThat(document.getScore()).isBetween(42f, 42f);
        assertThat(document.get("field")).isEqualTo("value");
        assertThat(document.hasSeqNo()).isTrue();
        assertThat(document.getSeqNo()).isEqualTo(1);
        assertThat(document.hasPrimaryTerm()).isTrue();
        assertThat(document.getPrimaryTerm()).isEqualTo(2);
    }

    @Test // DATAES-628
    public void searchResponseShouldReturnContainsKey() {

        Map<String, DocumentField> fields = new LinkedHashMap<>();

        fields.put("string", new DocumentField("string", Collections.singletonList("value")));
        fields.put("bool", new DocumentField("bool", Arrays.asList(true, true, false)));

        SearchHit searchHit = new SearchHit(123, "my-id", fields, null);

        SearchDocument document = DocumentAdapters.from(searchHit);

        assertThat(document.containsKey("string")).isTrue();
        assertThat(document.containsKey("not-set")).isFalse();
    }

    @Test // DATAES-628
    public void searchResponseShouldReturnContainsValue() {

        Map<String, DocumentField> fields = new LinkedHashMap<>();

        fields.put("string", new DocumentField("string", Collections.singletonList("value")));
        fields.put("bool", new DocumentField("bool", Arrays.asList(true, true, false)));
        fields.put("null", new DocumentField("null", Collections.emptyList()));

        SearchHit searchHit = new SearchHit(123, "my-id", fields, null);

        SearchDocument document = DocumentAdapters.from(searchHit);

        assertThat(document.containsValue("value")).isTrue();
        assertThat(document.containsValue(Arrays.asList(true, true, false))).isTrue();
        assertThat(document.containsValue(null)).isTrue();
    }

    @Test // DATAES-628
    public void shouldRenderToJson() {

        Map<String, DocumentField> fields = new LinkedHashMap<>();

        fields.put("string", new DocumentField("string", Collections.singletonList("value")));
        fields.put("bool", new DocumentField("bool", Arrays.asList(true, true, false)));

        SearchHit searchHit = new SearchHit(123, "my-id", fields, null);

        SearchDocument document = DocumentAdapters.from(searchHit);

        assertThat(document.toJson()).isEqualTo("{\"string\":\"value\",\"bool\":[true,true,false]}");
    }

    @Test // DATAES-628, DATAES-848
    public void shouldAdaptSearchResponseSource() {

        BytesArray source = new BytesArray("{\"field\":\"value\"}");

        SearchShardTarget shard = new SearchShardTarget("node", new ShardId("index", "uuid", 42), null, null);
        SearchHit searchHit = new SearchHit(123, "my-id", null, null);
        searchHit.shard(shard);
        searchHit.sourceRef(source).score(42);
        searchHit.version(22);
        searchHit.setSeqNo(1);
        searchHit.setPrimaryTerm(2);

        SearchDocument document = DocumentAdapters.from(searchHit);

        assertThat(document.getIndex()).isEqualTo("index");
        assertThat(document.hasId()).isTrue();
        assertThat(document.getId()).isEqualTo("my-id");
        assertThat(document.hasVersion()).isTrue();
        assertThat(document.getVersion()).isEqualTo(22);
        assertThat(document.getScore()).isBetween(42f, 42f);
        assertThat(document.get("field")).isEqualTo("value");
        assertThat(document.hasSeqNo()).isTrue();
        assertThat(document.getSeqNo()).isEqualTo(1);
        assertThat(document.hasPrimaryTerm()).isTrue();
        assertThat(document.getPrimaryTerm()).isEqualTo(2);
    }

    @Test // #725
    @DisplayName("should adapt returned explanations")
    void shouldAdaptReturnedExplanations() {

        SearchHit searchHit = new SearchHit(42);
        searchHit.explanation(org.apache.lucene.search.Explanation.match( //
                3.14, //
                "explanation 3.14", //
                Collections.singletonList(org.apache.lucene.search.Explanation.noMatch( //
                        "explanation noMatch", //
                        Collections.emptyList()))));

        SearchDocument searchDocument = DocumentAdapters.from(searchHit);

        Explanation explanation = searchDocument.getExplanation();
        assertThat(explanation).isNotNull();
        assertThat(explanation.isMatch()).isTrue();
        assertThat(explanation.getValue()).isEqualTo(3.14);
        assertThat(explanation.getDescription()).isEqualTo("explanation 3.14");
        List<Explanation> details = explanation.getDetails();
        assertThat(details)
                .containsExactly(new Explanation(false, 0.0, "explanation noMatch", Collections.emptyList()));
    }

    @Test // DATAES-979
    @DisplayName("should adapt returned matched queries")
    void shouldAdaptReturnedMatchedQueries() {
        SearchHit searchHit = new SearchHit(42);
        searchHit.matchedQueries(new String[] {"query1", "query2"});

        SearchDocument searchDocument = DocumentAdapters.from(searchHit);

        List<String> matchedQueries = searchDocument.getMatchedQueries();
        assertThat(matchedQueries).isNotNull();
        assertThat(matchedQueries).hasSize(2);
        assertThat(matchedQueries).isEqualTo(Arrays.asList("query1", "query2"));
    }
}
