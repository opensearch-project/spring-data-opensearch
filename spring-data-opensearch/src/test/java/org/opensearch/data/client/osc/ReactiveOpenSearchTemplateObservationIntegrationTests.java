/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.core.IndexOperationsAdapter.blocking;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.data.client.junit.jupiter.ReactiveOpenSearchTemplateConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.routing.RoutingResolver;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringIntegrationTest
@ContextConfiguration(classes = { ReactiveOpenSearchTemplateObservationIntegrationTests.Config.class })
@DisplayName("Using Reactive OpenSearch Client with Observations (CustomConvention)")
class ReactiveOpenSearchTemplateObservationIntegrationTests {

    @Autowired private IndexNameProvider indexNameProvider;
    @Autowired private ReactiveElasticsearchOperations operations;
    @Autowired private ObservationRecorder recorder;
    private ReactiveIndexOperations indexOperations;

    @Configuration
    @Import({ ReactiveOpenSearchTemplateConfiguration.class })
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("integration-observed-reactive-os");
        }

        @Bean
        ObservationRecorder observationRecorder() {
            return new ObservationRecorder();
        }

        @Bean
        ObservationRegistry observationRegistry(ObservationRecorder recorder) {
            return recorder.registry;
        }

        @Bean
        OpenSearchObservationConvention convention() {
            return new CustomConvention();
        }
    }

    @BeforeEach
    public void before() {
        indexNameProvider.increment();
        indexOperations = operations.indexOps(Sample.class);
        StepVerifier.create(indexOperations.createWithMapping()).expectNextMatches(s -> true).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.INDEX_CREATE);
        recorder.clear();
    }

    @AfterEach
    public void after() {
        recorder.clear();
    }

    @Test
    @Order(java.lang.Integer.MAX_VALUE)
    void cleanup() {
        blocking(operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*'))).delete();
    }

    @Test
    void existsUsesSubscriberParentAndCreatesOneObservationPerSubscription() {
        final var parent = Observation.start("caller", recorder.registry);
        operations.save(new Sample("1"), IndexCoordinates.of(indexNameProvider.indexName())).block();

        final var publisher = operations.exists("1", IndexCoordinates.of(indexNameProvider.indexName()))
                .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, parent));
        StepVerifier.create(publisher).expectNext(true).verifyComplete();
        StepVerifier.create(publisher).expectNext(true).verifyComplete();
        parent.stop();
        recorder.assertOperations(OpenSearchOperationName.SAVE, OpenSearchOperationName.EXISTS, OpenSearchOperationName.EXISTS);
        assertThat(recorder.started.get(0)).isNotSameAs(recorder.started.get(1));
    }

    @Test
    void registryAndConventionSurviveBothCopyPaths() {
        StepVerifier.create(operations.withRefreshPolicy(RefreshPolicy.IMMEDIATE).exists("1", IndexCoordinates.of(indexNameProvider.indexName())))
                .expectNext(false).verifyComplete();

        StepVerifier.create(operations.withRouting(RoutingResolver.just("route")).exists("1", IndexCoordinates.of(indexNameProvider.indexName())))
                .expectNext(false).verifyComplete();

        recorder.assertOperations(OpenSearchOperationName.EXISTS, OpenSearchOperationName.EXISTS);
            assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command.custom"));
    }

    @Test
    void cancellationStopsTemplateObservation() {
        StepVerifier.create(operations.exists("1", IndexCoordinates.of(indexNameProvider.indexName()))).thenCancel().verify();
        recorder.assertOperations(OpenSearchOperationName.EXISTS);
        assertThat(recorder.stopped.getFirst().getError()).isNull();
    }

    @Test
    void argumentValidationRemainsSynchronous() {
        assertThatIllegalArgumentException().isThrownBy(() -> operations.save((Object) null, IndexCoordinates.of(indexNameProvider.indexName())));
        assertThatIllegalArgumentException().isThrownBy(() -> operations.exists(null, IndexCoordinates.of(indexNameProvider.indexName())));
        assertThatIllegalArgumentException().isThrownBy(() -> operations.delete((String) null, IndexCoordinates.of(indexNameProvider.indexName())));
        assertThatIllegalArgumentException().isThrownBy(() -> operations.count(null, Sample.class, IndexCoordinates.of(indexNameProvider.indexName())));
        assertThatIllegalArgumentException().isThrownBy(() -> operations.search(null, Sample.class, IndexCoordinates.of(indexNameProvider.indexName())));
        recorder.assertOperations();
    }

    @Test
    void saveAllKeepsStreamingAndReportsBatchSize() {
        final var first = new Sample("1");
        StepVerifier.create(operations.saveAll(Mono.just(List.of(first, new Sample("2"))), Sample.class), 1)
                .expectNext(first).thenCancel().verify(Duration.ofSeconds(5));
        recorder.assertOperations(OpenSearchOperationName.BULK);
        assertThat(recorder.stopped.getFirst().getBatchSize()).isEqualTo(2);
    }

    @Test
    void bulkUpdateRecordsItsBatchAndFailures() {
        operations.save(new Sample("1"), IndexCoordinates.of(indexNameProvider.indexName())).block();
        recorder.assertOperations(OpenSearchOperationName.SAVE);
        recorder.clear();

        final var update = UpdateQuery.builder("1").withDocument(Document.create()).build();
        StepVerifier.create(operations.bulkUpdate(List.of(update, update), BulkOptions.defaultOptions(), IndexCoordinates.of(indexNameProvider.indexName())))
                .verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.BULK);
        assertThat(recorder.stopped.getFirst().getBatchSize()).isEqualTo(2);
        assertThat(recorder.stopped.getFirst().getError()).isNull();
    }

    @Test
    void responseSearchApisEachRecordExactlyOneSearch() {
        final var query = NativeQuery.builder().withQuery(q -> q.matchAll(m -> m)).build();
        StepVerifier.create(operations.searchForPage(query, Sample.class))
                .expectNextCount(1).verifyComplete();
        StepVerifier.create(operations.searchForHits(query, Sample.class))
                .expectNextCount(1).verifyComplete();
        StepVerifier.create(operations.aggregate(query, Sample.class)).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.SEARCH, OpenSearchOperationName.SEARCH, OpenSearchOperationName.SEARCH);
    }

    @Test
    void unboundedSearchNestsPitLifecycleUnderSearch() {
        operations.save(new Sample("1"), IndexCoordinates.of(indexNameProvider.indexName())).block();

        final var query = NativeQuery.builder().withQuery(q -> q.matchAll(m -> m)).withPageable(org.springframework.data.domain.Pageable.unpaged()).build();
        StepVerifier.create(operations.search(query, Sample.class)).expectNextMatches(s -> s.getId().equals("1")).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.SAVE, OpenSearchOperationName.SEARCH, OpenSearchOperationName.OPEN_POINT_IN_TIME, OpenSearchOperationName.CLOSE_POINT_IN_TIME);

        final var parent = recorder.started.get(1);
        assertThat(recorder.started.subList(2, 4)).allSatisfy(context ->
                assertThat(context.getParentObservation().getContextView()).isSameAs(parent));
    }

    @Test
    void submissionObservationDoesNotWaitForBackgroundTask() {
        StepVerifier.create(operations.submitReindex(reindex(indexNameProvider.indexName())))
                .expectNextMatches(s -> !s.isBlank()).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.SUBMIT_REINDEX);
        assertThat(recorder.stopped.getFirst().getIndexName()).isEqualTo(indexNameProvider.indexName());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void reindexFailuresRecordSourceIndicesOnlyInTraces(final boolean submission) {
        final var source = IndexCoordinates.of("source-one", "source-two");
        final var request = org.springframework.data.elasticsearch.core.reindex.ReindexRequest
                .builder(source, IndexCoordinates.of("target")).build();
        final var publisher = submission ? operations.submitReindex(request) : operations.reindex(request);
        assertThat(recorder.started).isEmpty();

        if (!submission) {
            StepVerifier.create(publisher).expectError().verify();
        } else {
            StepVerifier.create(publisher).expectNextMatches(s -> true).verifyComplete();
        }

        recorder.assertOperations(submission ? OpenSearchOperationName.SUBMIT_REINDEX : OpenSearchOperationName.REINDEX);
        final var context = recorder.stopped.getFirst();
        assertThat(context.getIndexCoordinates()).isSameAs(source);
        assertThat(context.getHighCardinalityKeyValue("spring.data.collection").getValue()).isEqualTo("source-one,source-two");
        assertThat(context.getLowCardinalityKeyValues()).hasSize(1);
        assertThat(context.getLowCardinalityKeyValue("spring.data.collection")).isNull();
        if (!submission) {
            assertThat(context.getError()).isNotNull();
        }
    }

    private static ReindexRequest reindex(String indexName) {
        return ReindexRequest.builder(IndexCoordinates.of(indexName),
            IndexCoordinates.of(indexName + "-target")).build();
    }

    @org.springframework.data.elasticsearch.annotations.Document(indexName = "#{@indexNameProvider.indexName()}")
    static class Sample {
        @Id private final String id;

        Sample(final String id) {
            this.id = id;
        }
    }

    static class CustomConvention extends DefaultOpenSearchObservationConvention {
        @Override
        public String getName() {
            return "spring.data.opensearch.command.custom";
        }
    }
}
