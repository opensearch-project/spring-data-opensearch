/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.jackson3.JacksonJsonpMapper;
import org.opensearch.client.opensearch._types.Refresh;
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

class BulkRequestConversionTests {

    private final RequestConverter converter = new RequestConverter(
            new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext()), new JacksonJsonpMapper());
    private final IndexCoordinates index = IndexCoordinates.of("default-index");

    @Test
    void shouldReuseBulkRequestSettingsWithoutOperations() {
        final var options = BulkOptions.builder().withTimeout(Duration.ofSeconds(2)).withPipeline("pipeline")
                .withRoutingId("route").withRefreshPolicy(RefreshPolicy.WAIT_UNTIL).build();
        final var operation = converter.bulkOperation(new IndexQueryBuilder().withId("id")
                .withObject(Map.of("message", "test")).build(), index);
        final var request = converter.bulkRequestBuilder(options, RefreshPolicy.IMMEDIATE)
                .operations(operation).build();

        assertThat(request.refresh()).isEqualTo(Refresh.WaitFor);
        assertThat(request.timeout().time()).isEqualTo("2000ms");
        assertThat(request.pipeline()).isEqualTo("pipeline");
        assertThat(request.routing()).isEqualTo("route");
        assertThat(request.operations()).containsExactly(operation);
    }

    @Test
    void shouldPreserveIndexCreateAndUpdateMetadata() {
        final var indexQuery = new IndexQueryBuilder().withId("id").withObject(Map.of("message", "test"))
                .withIndex("custom-index").withRouting("route").withSeqNoPrimaryTerm(new SeqNoPrimaryTerm(4, 2))
                .build();
        final var createQuery = new IndexQueryBuilder().withId("create").withObject(Map.of("message", "test"))
                .withOpType(IndexQuery.OpType.CREATE).build();
        final var updateQuery = UpdateQuery.builder("update").withDocument(Document.from(Map.of("message", "updated")))
                .withRetryOnConflict(3).withRouting("update-route").build();
        final var request = converter.documentBulkRequest(List.of(indexQuery, createQuery, updateQuery),
                BulkOptions.defaultOptions(), index, RefreshPolicy.IMMEDIATE);

        assertThat(request.operations()).hasSize(3);
        final var indexOperation = request.operations().getFirst().index();
        assertThat(indexOperation.index()).isEqualTo("custom-index");
        assertThat(indexOperation.id()).isEqualTo("id");
        assertThat(indexOperation.routing()).isEqualTo("route");
        assertThat(indexOperation.ifSeqNo()).isEqualTo(4L);
        assertThat(indexOperation.ifPrimaryTerm()).isEqualTo(2L);
        assertThat(request.operations().get(1).create().id()).isEqualTo("create");
        assertThat(request.operations().get(2).update().retryOnConflict()).isEqualTo(3);
        assertThat(request.operations().get(2).update().routing()).isEqualTo("update-route");
    }

    @Test
    void shouldPreserveExplicitIdsWhenIndexingAndCreatingFromJsonSource() {
        for (final var opType : IndexQuery.OpType.values()) {
            final var query = new IndexQueryBuilder().withId("explicit-id").withSource("{\"message\":\"test\"}")
                    .withOpType(opType).build();
            final var operation = converter.bulkOperation(query, index);
            assertThat(operation.isCreate() ? operation.create().id() : operation.index().id())
                    .isEqualTo("explicit-id");
        }
    }
}
