/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.routing.RoutingResolver;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveOpenSearchTemplateObservationTests {
    private static final IndexCoordinates INDEX = IndexCoordinates.of("observed");
    private final ReactiveOpenSearchClient client = mock(ReactiveOpenSearchClient.class, RETURNS_DEEP_STUBS);
    private final ObservationRecorder recorder = new ObservationRecorder();
    private final MappingElasticsearchConverter converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
    private ReactiveOpenSearchTemplate template;

    @BeforeEach
    void setUp() {
        when(client._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        template = new ReactiveOpenSearchTemplate(client, converter);
        template.setObservationRegistry(recorder.registry);
    }

    @Test
    void existsUsesSubscriberParentAndCreatesOneObservationPerSubscription() {
        final var parent = Observation.start("caller", recorder.registry);
        when(client.exists(any(ExistsRequest.class))).thenAnswer(invocation -> Mono.deferContextual(context -> {
            final Observation child = context.get(ObservationThreadLocalAccessor.KEY);
            assertThat(child.getContext().getParentObservation()).isSameAs(parent);
            return Mono.just(new BooleanResponse(true));
        }));
        final var publisher = template.exists("1", INDEX)
                .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, parent));
        assertThat(recorder.started).isEmpty();
        StepVerifier.create(publisher).expectNext(true).verifyComplete();
        StepVerifier.create(publisher).expectNext(true).verifyComplete();
        parent.stop();
        recorder.assertOperations(OpenSearchOperationName.EXISTS, OpenSearchOperationName.EXISTS);
        assertThat(recorder.started.get(0)).isNotSameAs(recorder.started.get(1));
    }

    @Test
    void registryAndConventionSurviveBothCopyPaths() {
        when(client.exists(any(ExistsRequest.class))).thenReturn(Mono.just(new BooleanResponse(true)));
        template = new ReactiveOpenSearchTemplate(client, converter);
        try (final var context = new StaticApplicationContext()) {
            context.getBeanFactory().registerSingleton("observationRegistry", recorder.registry);
            context.getBeanFactory().registerSingleton("convention", new OpenSearchTemplateObservationTests.CustomConvention());
            template.setApplicationContext(context);
            StepVerifier.create(template.withRefreshPolicy(RefreshPolicy.IMMEDIATE).exists("1", INDEX))
                    .expectNext(true).verifyComplete();
            StepVerifier.create(template.withRouting(RoutingResolver.just("route")).exists("1", INDEX))
                    .expectNext(true).verifyComplete();
        }
        recorder.assertOperations(OpenSearchOperationName.EXISTS, OpenSearchOperationName.EXISTS);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("custom.opensearch"));
    }

    @Test
    void cancellationStopsTemplateObservation() {
        when(client.exists(any(ExistsRequest.class))).thenReturn(Mono.never());
        StepVerifier.create(template.exists("1", INDEX)).thenCancel().verify();
        recorder.assertOperations(OpenSearchOperationName.EXISTS);
        assertThat(recorder.stopped.getFirst().getError()).isNull();
    }

    @Test
    void errorStopsTemplateObservation() {
        when(client.exists(any(ExistsRequest.class))).thenReturn(Mono.error(new IllegalStateException("failure")));
        StepVerifier.create(template.exists("1", INDEX)).expectError().verify();
        recorder.assertOperations(OpenSearchOperationName.EXISTS);
        assertThat(recorder.stopped.getFirst().getError()).isNotNull();
    }

    @Test
    void argumentValidationRemainsSynchronous() {
        assertThatIllegalArgumentException().isThrownBy(() -> template.save((Object) null, INDEX));
        assertThatIllegalArgumentException().isThrownBy(() -> template.exists(null, INDEX));
        assertThatIllegalArgumentException().isThrownBy(() -> template.delete((String) null, INDEX));
        assertThatIllegalArgumentException().isThrownBy(() -> template.count(null, null, INDEX));
        assertThatIllegalArgumentException().isThrownBy(() -> template.search(null, Object.class, Object.class, INDEX));
        recorder.assertOperations();
    }

    @Test
    void saveAllKeepsStreamingAndReportsBatchSize() {
        final var item = BulkResponseItem.of(response -> response.operationType(org.opensearch.client.opensearch.core.bulk.OperationType.Index)
                .id("1").index("observed").status(201).seqNo(1L).primaryTerm(1L).version(1L));
        when(client.bulk(any(BulkRequest.class))).thenReturn(Mono.just(BulkResponse.of(response ->
                response.errors(false).took(1).items(item, item))));
        template = new ReactiveOpenSearchTemplate(client, converter) {
            @Override
            protected <T> Mono<T> maybeCallbackAfterSave(final T entity, final IndexCoordinates index) {
                return entity instanceof OpenSearchTemplateObservationTests.Sample sample && sample == second
                        ? Mono.never() : Mono.just(entity);
            }
        };
        template.setObservationRegistry(recorder.registry);
        final var first = new OpenSearchTemplateObservationTests.Sample("1");
        StepVerifier.create(template.saveAll(Mono.just(List.of(first, second)), INDEX), 1)
                .expectNext(first).thenCancel().verify(Duration.ofSeconds(5));
        recorder.assertOperations(OpenSearchOperationName.BULK);
        assertThat(recorder.stopped.getFirst().getBatchSize()).isEqualTo(2);
    }

    private final OpenSearchTemplateObservationTests.Sample second = new OpenSearchTemplateObservationTests.Sample("2");

    @Test
    void bulkUpdateRecordsItsBatchAndFailures() {
        when(client.bulk(any(BulkRequest.class))).thenReturn(Mono.error(new IllegalStateException("bulk failure")));
        final var update = UpdateQuery.builder("1").withDocument(Document.create()).build();
        StepVerifier.create(template.bulkUpdate(List.of(update, update), BulkOptions.defaultOptions(), INDEX))
                .expectError().verify();
        recorder.assertOperations(OpenSearchOperationName.BULK);
        assertThat(recorder.stopped.getFirst().getBatchSize()).isEqualTo(2);
        assertThat(recorder.stopped.getFirst().getError()).isNotNull();
    }

    @Test
    void responseSearchApisEachRecordExactlyOneSearch() {
        final var response = new SearchResponse.Builder<EntityAsMap>().took(1).timedOut(false)
                .shards(shards -> shards.total(1).successful(1).failed(0))
                .hits(hits -> hits.hits(List.of()).total(total -> total.value(0).relation(TotalHitsRelation.Eq))).build();
        when(client.search(any(SearchRequest.class), eq(EntityAsMap.class))).thenReturn(Mono.just(response));
        final var query = NativeQuery.builder().withQuery(q -> q.matchAll(m -> m)).build();
        StepVerifier.create(template.searchForPage(query, OpenSearchTemplateObservationTests.Sample.class, INDEX))
                .expectNextCount(1).verifyComplete();
        StepVerifier.create(template.searchForHits(query, OpenSearchTemplateObservationTests.Sample.class, INDEX))
                .expectNextCount(1).verifyComplete();
        StepVerifier.create(template.aggregate(query, OpenSearchTemplateObservationTests.Sample.class, INDEX)).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.SEARCH, OpenSearchOperationName.SEARCH, OpenSearchOperationName.SEARCH);
    }

    @Test
    void unboundedSearchNestsPitLifecycleUnderSearch() {
        when(client.openPointInTime(any(CreatePitRequest.class))).thenReturn(Mono.just(CreatePitResponse.of(pit ->
                pit.pitId("pit").creationTime(1L).shards(shards -> shards.total(1).successful(1).failed(0)))));
        final var response = new SearchResponse.Builder<EntityAsMap>().took(1).timedOut(false)
                .shards(shards -> shards.total(1).successful(1).failed(0))
                .hits(hits -> hits.hits(List.of())).build();
        when(client.search(any(SearchRequest.class), eq(EntityAsMap.class))).thenReturn(Mono.just(response));
        when(client.closePointInTime(any(DeletePitRequest.class))).thenReturn(Mono.just(DeletePitResponse.of(pit ->
                pit.pits(result -> result.pitId("pit").successful(true)))));
        final var query = NativeQuery.builder().withQuery(q -> q.matchAll(m -> m)).withPageable(org.springframework.data.domain.Pageable.unpaged()).build();
        StepVerifier.create(template.search(query, OpenSearchTemplateObservationTests.Sample.class, INDEX)).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.SEARCH, OpenSearchOperationName.OPEN_POINT_IN_TIME, OpenSearchOperationName.CLOSE_POINT_IN_TIME);
        final var parent = recorder.started.getFirst();
        assertThat(recorder.started.subList(1, 3)).allSatisfy(context ->
                assertThat(context.getParentObservation().getContextView()).isSameAs(parent));
    }

    @Test
    void submissionObservationDoesNotWaitForBackgroundTask() {
        when(client.reindex(any(org.opensearch.client.opensearch.core.ReindexRequest.class)))
                .thenReturn(Mono.just(ReindexResponse.of(response -> response.task("node:1"))));
        StepVerifier.create(template.submitReindex(OpenSearchTemplateObservationTests.reindex()))
                .expectNext("node:1").verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.SUBMIT_REINDEX);
    }
}
