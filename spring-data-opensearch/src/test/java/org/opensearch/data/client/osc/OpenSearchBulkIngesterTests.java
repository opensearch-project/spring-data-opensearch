/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.opensearch.client.ResponseException;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.bulk.OperationType;
import org.opensearch.client.transport.OpenSearchTransport;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterListener;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterOptions;
import org.springframework.data.elasticsearch.core.bulk.BulkOperationResult;
import org.springframework.data.elasticsearch.core.bulk.BulkOperationType;
import org.springframework.data.elasticsearch.core.event.AfterSaveCallback;
import org.springframework.data.elasticsearch.core.event.BeforeConvertCallback;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.mapping.callback.EntityCallbacks;

@Timeout(10)
class OpenSearchBulkIngesterTests {

    private static final IndexCoordinates INDEX = IndexCoordinates.of("test");

    private final OpenSearchTransport transport = mock(OpenSearchTransport.class);
    private final List<BulkRequest> requests = new CopyOnWriteArrayList<>();
    private final RecordingListener<String> listener = new RecordingListener<>();
    private OpenSearchTemplate template;

    @BeforeEach
    void setUp() throws Exception {
        when(transport.jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        template = new OpenSearchTemplate(new OpenSearchClient(transport));
        when(transport.performRequestAsync(any(BulkRequest.class), any(), any())).thenAnswer(invocation -> {
            final BulkRequest request = invocation.getArgument(0);
            requests.add(request);
            return CompletableFuture.completedFuture(successResponse(request));
        });
    }

    @Test
    void shouldSendMixedOperationsWithRequestSettingsAndContexts() throws Exception {
        template.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
        final var bulkOptions = BulkOptions.builder()
                .withTimeout(Duration.ofSeconds(2))
                .withRefreshPolicy(RefreshPolicy.NONE)
                .withPipeline("pipeline")
                .withRoutingId("request-route")
                .build();
        final var options = BulkIngesterOptions.builder().withMaxOperations(4).withBulkOptions(bulkOptions).build();

        try (final var ingester = template.bulkIngester(options, INDEX, listener)) {
            ingester.add(new IndexQueryBuilder().withId("1").withSource("{\"message\":\"one\"}").build(), "index");
            ingester.add(new IndexQueryBuilder().withId("2").withSource("{}")
                    .withOpType(org.springframework.data.elasticsearch.core.query.IndexQuery.OpType.CREATE).build(), "create");
            ingester.add(UpdateQuery.builder("3")
                    .withDocument(org.springframework.data.elasticsearch.core.document.Document.from(java.util.Map.of("message", "updated")))
                    .build(), "update");
            ingester.addDelete("4", "delete-route", "delete");
        }

        assertThat(requests).hasSize(1);
        final var request = requests.getFirst();
        assertThat(request.timeout().time()).isEqualTo("2000ms");
        assertThat(request.refresh()).isEqualTo(Refresh.False);
        assertThat(request.pipeline()).isEqualTo("pipeline");
        assertThat(request.routing()).isEqualTo("request-route");
        assertThat(request.operations()).extracting(operation -> operation._kind().jsonValue())
                .containsExactly("index", "create", "update", "delete");
        assertThat(request.operations().getFirst().index().id()).isEqualTo("1");
        assertThat(request.operations().get(1).create().id()).isEqualTo("2");
        assertThat(request.operations().get(2).update().id()).isEqualTo("3");
        assertThat(request.operations().getLast().delete().id()).isEqualTo("4");
        assertThat(request.operations().getLast().delete().routing()).isEqualTo("delete-route");
        assertThat(request.operations().getLast().delete().index()).isEqualTo("test");
        assertThat(listener.beforeContexts).containsExactly(List.of("index", "create", "update", "delete"));
        assertThat(listener.results).extracting(BulkOperationResult::context).containsExactly("index", "create", "update", "delete");
        assertThat(listener.results).extracting(BulkOperationResult::operationType)
                .containsExactly(BulkOperationType.INDEX, BulkOperationType.CREATE, BulkOperationType.UPDATE, BulkOperationType.DELETE);
        assertThat(listener.results).allMatch(BulkOperationResult::isSuccessful);
        assertThat(listener.failures).isEmpty();
    }

    @Test
    void shouldApplyCallbacksAndWriteBackOnlySuccessfulEntities() throws Exception {
        final var saved = new CopyOnWriteArrayList<SampleEntity>();
        final BeforeConvertCallback<SampleEntity> before = (entity, index) -> {
            entity.message = "before";
            return entity;
        };
        final AfterSaveCallback<SampleEntity> after = (entity, index) -> {
            saved.add(entity);
            return entity;
        };
        template.setEntityCallbacks(EntityCallbacks.create(before, after));
        when(transport.performRequestAsync(any(BulkRequest.class), any(), any())).thenReturn(
                CompletableFuture.completedFuture(BulkResponse.of(response -> response.took(1).errors(true).items(
                        BulkResponseItem.of(item -> item.operationType(OperationType.Index).index("test").id("generated")
                                .status(201).seqNo(7L).primaryTerm(2L).version(3L)),
                        BulkResponseItem.of(item -> item.operationType(OperationType.Create).index("test").id("existing")
                                .status(409).error(error -> error.type("version_conflict_engine_exception")))))));
        final var successful = new SampleEntity();
        final var failed = new SampleEntity();
        final var successQuery = new IndexQueryBuilder().withObject(successful).build();

        try (final var ingester = template.bulkIngester(INDEX, listener)) {
            ingester.add(successQuery, "success");
            ingester.add(new IndexQueryBuilder().withId("existing").withObject(failed)
                    .withOpType(org.springframework.data.elasticsearch.core.query.IndexQuery.OpType.CREATE).build(), "failure");
            assertThat(successful.message).isEqualTo("before");
            assertThat(failed.message).isEqualTo("before");
            assertThat(saved).isEmpty();
        }

        assertThat(successful.id).isEqualTo("generated");
        assertThat(successful.version).isEqualTo(3L);
        assertThat(successful.seqNoPrimaryTerm).isEqualTo(new SeqNoPrimaryTerm(7L, 2L));
        assertThat(saved).containsExactly(successful);
        assertThat(failed.id).isNull();
        assertThat(failed.version).isNull();
        assertThat(failed.seqNoPrimaryTerm).isNull();
        assertThat(listener.results).hasSize(2);
        assertThat(listener.results.getFirst().indexedObjectInformation().id()).isEqualTo("generated");
        final var failure = listener.results.getLast();
        assertThat(failure.isSuccessful()).isFalse();
        assertThat(failure.status()).isEqualTo(409);
        assertThat(failure.failure().errorMessage()).isEqualTo("version_conflict_engine_exception");
        assertThat(listener.failures).isEmpty();
    }

    @Test
    void shouldTranslateWrappedRequestFailureWithoutRetrying() throws Exception {
        final var failure = new OpenSearchException(
                org.opensearch.client.opensearch._types.ErrorResponse.of(response -> response.status(404)
                        .error(error -> error.type("index_not_found_exception").reason("no such index [test]"))));
        when(transport.performRequestAsync(any(BulkRequest.class), any(), any())).thenReturn(
                CompletableFuture.failedFuture(new CompletionException(new ExecutionException(failure))));

        try (final var ingester = template.bulkIngester(INDEX, listener)) {
            ingester.addDelete("1", null, "context");
        }

        assertThat(listener.results).isEmpty();
        assertThat(listener.failureContexts).containsExactly(List.of("context"));
        assertThat(listener.failures).singleElement().isInstanceOf(NoSuchIndexException.class);
        verify(transport).performRequestAsync(any(BulkRequest.class), any(), any());
    }

    @Test
    void shouldPreserveConflictExceptionsThrownByTranslator() throws Exception {
        final var failure = mock(ResponseException.class, RETURNS_DEEP_STUBS);
        when(failure.getResponse().getStatusLine().getStatusCode()).thenReturn(409);
        when(failure.getMessage()).thenReturn(
                "{\"type\":\"version_conflict_engine_exception\",\"reason\":\"version conflict, required seqNo [1]\"}");
        when(transport.performRequestAsync(any(BulkRequest.class), any(), any())).thenReturn(
                CompletableFuture.failedFuture(failure));

        try (final var ingester = template.bulkIngester(INDEX, listener)) {
            ingester.addDelete("1");
        }

        assertThat(listener.failures).singleElement().isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void shouldFlushPendingOperationsOnCloseAndKeepClientOpen() throws Exception {
        template.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL);
        final var ingester = template.bulkIngester(INDEX, listener);
        try (ingester) {
            ingester.add(new IndexQueryBuilder().withSource("{}").build());
            assertThat(ingester.getPendingOperations()).isEqualTo(1);
            assertThat(ingester.getPendingOperationsSize()).isPositive();
            assertThat(ingester.getOperationsCount()).isEqualTo(1);
            assertThat(requests).isEmpty();
        }
        ingester.close();

        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst().refresh()).isEqualTo(Refresh.WaitFor);
        assertThat(listener.results).singleElement().satisfies(result -> assertThat(result.context()).isNull());
        assertThat(ingester.getPendingOperations()).isZero();
        assertThat(ingester.getPendingRequests()).isZero();
        assertThat(ingester.getRequestCount()).isEqualTo(1);
        assertThatThrownBy(() -> ingester.addDelete("2")).isInstanceOf(IllegalStateException.class);
        verify(transport, never()).close();
    }

    @Test
    void shouldFlushAtConfiguredSizeLimit() {
        final var options = BulkIngesterOptions.builder().withMaxSize(1).build();
        try (final var ingester = template.bulkIngester(options, INDEX, listener)) {
            ingester.addDelete("1");
            assertThat(requests).hasSize(1);
        }
    }

    @Test
    void shouldFlushAtConfiguredInterval() throws Exception {
        final var options = BulkIngesterOptions.builder().withFlushInterval(Duration.ofMillis(20)).build();
        try (final var ingester = template.bulkIngester(options, INDEX, listener)) {
            ingester.addDelete("1");
            assertThat(listener.completed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(requests).hasSize(1);
        }
    }

    @Test
    void shouldAllowConfiguredConcurrentRequests() throws Exception {
        final var pending = new CopyOnWriteArrayList<CompletableFuture<BulkResponse>>();
        when(transport.performRequestAsync(any(BulkRequest.class), any(), any())).thenAnswer(invocation -> {
            requests.add(invocation.getArgument(0));
            final var response = new CompletableFuture<BulkResponse>();
            pending.add(response);
            return response;
        });
        final var options = BulkIngesterOptions.builder().withMaxOperations(1).withMaxConcurrentRequests(2).build();
        try (final var ingester = template.bulkIngester(options, INDEX, listener)) {
            try {
                ingester.addDelete("1", null, "one");
                ingester.addDelete("2", null, "two");
                assertThat(requests).hasSize(2);
                assertThat(ingester.getPendingRequests()).isEqualTo(2);
            } finally {
                for (var i = 0; i < pending.size(); i++) {
                    pending.get(i).complete(successResponse(requests.get(i)));
                }
            }
        }
        assertThat(listener.results).extracting(BulkOperationResult::context).containsExactlyInAnyOrder("one", "two");
    }

    @Test
    void shouldRequireListener() {
        assertThatIllegalArgumentException().isThrownBy(() -> template.bulkIngester(INDEX, null))
                .withMessageContaining("listener");
    }

    private static BulkResponse successResponse(final BulkRequest request) {
        return BulkResponse.of(response -> response.took(1).errors(false).items(request.operations().stream()
                .map(operation -> BulkResponseItem.of(item -> item.operationType(switch (operation._kind()) {
                    case Index -> OperationType.Index;
                    case Create -> OperationType.Create;
                    case Update -> OperationType.Update;
                    case Delete -> OperationType.Delete;
                })
                        .index("test").id("id").status(200).seqNo(1L).primaryTerm(1L).version(1L)))
                .toList()));
    }

    private static class RecordingListener<C> implements BulkIngesterListener<C> {
        private final List<List<C>> beforeContexts = new CopyOnWriteArrayList<>();
        private final List<BulkOperationResult<C>> results = new CopyOnWriteArrayList<>();
        private final List<List<C>> failureContexts = new CopyOnWriteArrayList<>();
        private final List<RuntimeException> failures = new CopyOnWriteArrayList<>();
        private final CountDownLatch completed = new CountDownLatch(1);

        @Override
        public void beforeBulk(final long executionId, final List<C> contexts) {
            beforeContexts.add(contexts);
        }

        @Override
        public void afterBulk(final long executionId, final List<BulkOperationResult<C>> results) {
            this.results.addAll(results);
            completed.countDown();
        }

        @Override
        public void afterBulk(final long executionId, final List<C> contexts, final RuntimeException failure) {
            failureContexts.add(contexts);
            failures.add(failure);
            completed.countDown();
        }
    }

    @Document(indexName = "test")
    static class SampleEntity {
        @Id private String id;
        @Version private Long version;
        private SeqNoPrimaryTerm seqNoPrimaryTerm;
        private String message;
    }
}
