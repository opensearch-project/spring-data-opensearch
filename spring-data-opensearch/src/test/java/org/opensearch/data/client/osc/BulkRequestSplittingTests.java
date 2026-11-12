/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.NdJsonpSerializable;
import org.opensearch.client.json.jackson3.JacksonJsonpMapper;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

class BulkRequestSplittingTests {

    private final JacksonJsonpMapper mapper = new JacksonJsonpMapper();
    private final RequestConverter converter = new RequestConverter(
            new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()), mapper);
    private final IndexCoordinates index = IndexCoordinates.of("index");

    @Test
    void shouldKeepOneRequestWithoutLimitsIncludingEmptyInput() {
        final var queries = List.of(query("1", "one"), query("2", "two"));
        assertThat(requests(queries, BulkOptions.defaultOptions(), BulkOptions.defaultOptions()))
                .singleElement().satisfies(request -> assertThat(request.operations()).hasSize(2));
        assertThat(requests(List.of(), BulkOptions.builder().withMaxOperations(1).build(),
                BulkOptions.defaultOptions()))
                .singleElement().satisfies(request -> assertThat(request.operations()).isEmpty());
    }

    @Test
    void shouldSplitByCountAndKeepRequestSettings() {
        final var options = BulkOptions.builder().withMaxOperations(2).withTimeout(Duration.ofSeconds(2))
                .withPipeline("pipeline").withRoutingId("route").withRefreshPolicy(RefreshPolicy.WAIT_UNTIL).build();
        final var queries = List.of(query("1", "one"), query("2", "two"), query("3", "three"));
        final var requests = requests(queries, options, BulkOptions.builder().withMaxOperations(1).build());

        assertThat(requests).extracting(request -> request.operations().size()).containsExactly(2, 1);
        assertThat(requests).extracting(BulkRequest::refresh).containsExactly(Refresh.False, Refresh.WaitFor);
        assertThat(requests).allSatisfy(request -> {
            assertThat(request.timeout().time()).isEqualTo("2000ms");
            assertThat(request.pipeline()).isEqualTo("pipeline");
            assertThat(request.routing()).isEqualTo("route");
        });
        assertThat(requests.stream().flatMap(request -> request.operations().stream()))
                .extracting(operation -> operation.index().id()).containsExactly("1", "2", "3");
    }

    @Test
    void shouldMeasureExactUtf8SizeForIndexCreateAndUpdate() {
        final var indexQuery = query("1", "\u20ac\u00e9");
        final var createQuery = query("2", "\u20ac\u00e9");
        createQuery.setOpType(IndexQuery.OpType.CREATE);
        final var updateQuery = UpdateQuery.builder("3")
                .withDocument(Document.from(Map.of("message", "\u20ac\u00e9")))
                .withUpsert(Document.from(Map.of("message", "initial")))
                .withRouting("route").withRetryOnConflict(2).build();
        for (final var query : List.of(indexQuery, createQuery, updateQuery)) {
            final var singleRequest = converter.documentBulkRequest(
                    List.of(query), BulkOptions.defaultOptions(), index, RefreshPolicy.IMMEDIATE);
            final var size = bodySize(singleRequest);
            final var exact = requests(List.of(query, query), BulkOptions.builder().withMaxSize(size * 2L).build(),
                    BulkOptions.defaultOptions());
            final var below = requests(List.of(query, query), BulkOptions.builder().withMaxSize(size * 2L - 1).build(),
                    BulkOptions.defaultOptions());

            assertThat(exact).singleElement().satisfies(request -> {
                assertThat(request.operations()).hasSize(2);
                assertThat(bodySize(request)).isEqualTo(size * 2);
            });
            assertThat(below).hasSize(2).allSatisfy(request -> {
                assertThat(request.operations()).hasSize(1);
                assertThat(bodySize(request)).isEqualTo(size);
            });
        }
    }

    @Test
    void shouldSendOversizedOperationAloneWithoutChangingItsMetadata() {
        final var small = query("1", "small");
        final var large = new IndexQueryBuilder().withId("large").withObject(Map.of("message", "x".repeat(1000)))
                .withRouting("route").withSeqNoPrimaryTerm(new SeqNoPrimaryTerm(4, 2)).build();
        final var limit = bodySize(converter.documentBulkRequest(
                List.of(small), BulkOptions.defaultOptions(), index, null)) * 2L;
        final var requests = requests(List.of(small, large, small),
                BulkOptions.builder().withMaxSize(limit).build(), BulkOptions.defaultOptions());

        assertThat(requests).extracting(request -> request.operations().size()).containsExactly(1, 1, 1);
        assertThat(bodySize(requests.get(1))).isGreaterThan(limit);
        final var operation = requests.get(1).operations().getFirst().index();
        assertThat(operation.id()).isEqualTo("large");
        assertThat(operation.routing()).isEqualTo("route");
        assertThat(operation.ifSeqNo()).isEqualTo(4L);
        assertThat(operation.ifPrimaryTerm()).isEqualTo(2L);
    }

    @Test
    void shouldApplyBothLimitsAndFallBackIndependentlyToTemplateDefaults() {
        final var query = query("1", "small");
        final var size = bodySize(converter.documentBulkRequest(
                List.of(query), BulkOptions.defaultOptions(), index, null));
        final var queries = List.of(query, query, query);
        final var defaults = BulkOptions.builder().withMaxOperations(1).withMaxSize(size * 2L).build();

        assertThat(requests(queries, BulkOptions.builder().withMaxOperations(3).build(), defaults))
                .extracting(request -> request.operations().size()).containsExactly(2, 1);
        assertThat(requests(queries, BulkOptions.builder().withMaxSize(size * 3L).build(), defaults))
                .extracting(request -> request.operations().size()).containsExactly(1, 1, 1);
    }

    private List<BulkRequest> requests(final List<?> queries, final BulkOptions options, final BulkOptions defaults) {
        return converter.documentBulkRequests(queries, options, defaults, index, RefreshPolicy.IMMEDIATE);
    }

    private static IndexQuery query(final String id, final String message) {
        return new IndexQueryBuilder().withId(id).withObject(Map.of("message", message)).build();
    }

    private long bodySize(final BulkRequest request) {
        final var output = new ByteArrayOutputStream();
        writeNdJson(request, output);
        return output.size();
    }

    private void writeNdJson(final NdJsonpSerializable value, final ByteArrayOutputStream output) {
        final var items = value._serializables();
        while (items.hasNext()) {
            final var item = items.next();
            if (item instanceof NdJsonpSerializable nested && item != value) {
                writeNdJson(nested, output);
            } else {
                try (final var generator = mapper.jsonProvider().createGenerator(output)) {
                    mapper.serialize(item, generator);
                }
                output.write('\n');
            }
        }
    }
}
