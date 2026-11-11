/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class OpenSearchObservationTests {
    private static final IndexCoordinates INDEX = IndexCoordinates.of("first", "second");
    private final ObservationRegistry registry = ObservationRegistry.create();
    private final List<Observation.Context> started = new ArrayList<>();
    private final List<Observation.Context> stopped = new ArrayList<>();
    private final OpenSearchObservationSupport support = new OpenSearchObservationSupport(registry, null);

    OpenSearchObservationTests() {
        registry.observationConfig().observationHandler(new ObservationHandler<>() {
            @Override
            public boolean supportsContext(final Observation.Context context) {
                return true;
            }

            @Override
            public void onStart(final Observation.Context context) {
                started.add(context);
            }

            @Override
            public void onStop(final Observation.Context context) {
                stopped.add(context);
            }
        });
    }

    @Test
    void conventionKeepsUnboundedValuesOutOfMetrics() {
        final var context = new OpenSearchObservationContext(OpenSearchOperationName.BULK, INDEX);
        context.setBatchSize(42);
        final var convention = DefaultOpenSearchObservationConvention.INSTANCE;

        assertThat(context.getIndexCoordinates()).isSameAs(INDEX);
        assertThat(context.getIndexName()).isEqualTo("first,second");
        assertThat(convention.getName()).isEqualTo("spring.data.opensearch.command");
        assertThat(convention.getContextualName(context)).isEqualTo("bulk first,second");
        assertThat(convention.getLowCardinalityKeyValues(context))
                .containsExactly(KeyValue.of("spring.data.operation", "bulk"));
        assertThat(convention.getHighCardinalityKeyValues(context)).containsExactlyInAnyOrder(
                KeyValue.of("spring.data.collection", "first,second"), KeyValue.of("spring.data.batch.size", "42"));
        assertThat(convention.supportsContext(context)).isTrue();
        assertThat(convention.supportsContext(new Observation.Context())).isFalse();
    }

    @Test
    void conventionOmitsAbsentIndexAndBatchSize() {
        final var context = new OpenSearchObservationContext(OpenSearchOperationName.CLUSTER_HEALTH, null);
        assertThat(context.getIndexName()).isNull();
        assertThat(context.getBatchSize()).isNull();
        assertThat(DefaultOpenSearchObservationConvention.INSTANCE.getHighCardinalityKeyValues(context)).isEmpty();
        assertThat(DefaultOpenSearchObservationConvention.INSTANCE.getContextualName(context)).isEqualTo("clusterHealth");
    }

    @Test
    void metadataAgreesWithConvention() {
        final var documentation = OpenSearchObservation.OPENSEARCH_COMMAND_OBSERVATION;
        assertThat(documentation.getDefaultConvention()).isEqualTo(DefaultOpenSearchObservationConvention.class);
        assertThat(documentation.getLowCardinalityKeyNames()).extracting(key -> key.asString())
                .containsExactly("spring.data.operation");
        assertThat(documentation.getHighCardinalityKeyNames()).extracting(key -> key.asString())
                .containsExactlyInAnyOrder("spring.data.collection", "spring.data.batch.size");
        assertThat(OpenSearchOperationName.values()).extracting(OpenSearchOperationName::getValue).doesNotHaveDuplicates();
    }

    @Test
    void imperativeObservationOpensScopeAndRecordsErrors() {
        final var failure = new IllegalStateException("failure");
        assertThatThrownBy(() -> support.observe(OpenSearchOperationName.GET, INDEX, () -> {
            assertThat(registry.getCurrentObservation()).isNotNull();
            throw failure;
        })).isSameAs(failure);
        assertThat(started).hasSize(1);
        assertThat(stopped).containsExactlyElementsOf(started);
        assertThat(stopped.getFirst().getError()).isSameAs(failure);
        assertThat(registry.getCurrentObservation()).isNull();
    }

    @Test
    void reactiveObservationsAreLazyAndIsolatedPerSubscription() {
        final var calls = new AtomicInteger();
        final var publisher = support.mono(OpenSearchOperationName.EXISTS, INDEX,
                () -> Mono.just(calls.incrementAndGet()));
        assertThat(started).isEmpty();
        StepVerifier.create(publisher).expectNext(1).verifyComplete();
        StepVerifier.create(publisher).expectNext(2).verifyComplete();
        assertThat(started).hasSize(2);
        assertThat(started.get(0)).isNotSameAs(started.get(1));
        assertThat(stopped).containsExactlyElementsOf(started);
    }

    @Test
    void reactiveObservationUsesAndPublishesAccessorKey() {
        final var parent = Observation.start("parent", registry);
        final var publisher = support.mono(OpenSearchOperationName.EXISTS, INDEX, () -> Mono.deferContextual(context -> {
            final Observation child = context.get(ObservationThreadLocalAccessor.KEY);
            assertThat(child.getContext().getParentObservation()).isSameAs(parent);
            assertThat(context.hasKey(Observation.class)).isFalse();
            return Mono.just(true);
        }));
        StepVerifier.create(publisher.contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, parent)))
                .expectNext(true).verifyComplete();
        parent.stop();
        assertThat(started).hasSize(2);
        assertThat(stopped).hasSize(2);
    }

    @Test
    void siblingsUseCallerRatherThanPreviousChildAsParent() {
        final var parent = Observation.start("parent", registry);
        final var first = support.mono(OpenSearchOperationName.GET, INDEX, () -> Mono.just(1));
        final var second = support.mono(OpenSearchOperationName.GET, INDEX, () -> Mono.just(2));
        StepVerifier.create(first.then(second).contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, parent)))
                .expectNext(2).verifyComplete();
        parent.stop();
        assertThat(started.subList(1, 3)).allSatisfy(context -> assertThat(context.getParentObservation()).isSameAs(parent));
    }

    @Test
    void reactiveErrorsAndAssemblyFailuresStopObservation() {
        final var failure = new IllegalStateException("failure");
        StepVerifier.create(support.mono(OpenSearchOperationName.GET, INDEX, () -> Mono.error(failure)))
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure)).verify();
        StepVerifier.create(support.mono(OpenSearchOperationName.GET, INDEX, () -> { throw failure; }))
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure)).verify();
        assertThat(stopped).hasSize(2).allSatisfy(context -> assertThat(context.getError()).isSameAs(failure));
    }

    @Test
    void cancellationStopsObservationWithoutCollectingTheStream() {
        StepVerifier.create(support.flux(OpenSearchOperationName.BULK, INDEX, 2,
                () -> Flux.just(1, 2).concatWith(Flux.never())), 0)
                .thenRequest(1).expectNext(1).thenCancel().verify();
        assertThat(stopped).hasSize(1);
        assertThat(stopped.getFirst().getError()).isNull();
        assertThat(stopped.getFirst().getHighCardinalityKeyValue("spring.data.batch.size").getValue()).isEqualTo("2");
    }

    @Test
    void noopRegistryPreservesResults() {
        final var noop = new OpenSearchObservationSupport(ObservationRegistry.NOOP, null);
        assertThat(noop.observe(OpenSearchOperationName.GET, INDEX, () -> "value")).isEqualTo("value");
        StepVerifier.create(noop.flux(OpenSearchOperationName.SEARCH, INDEX, null, () -> Flux.just(1, 2)))
                .expectNext(1, 2).verifyComplete();
    }

    @Test
    void customConventionOverridesDefault() {
        final var custom = new OpenSearchObservationSupport(registry, new DefaultOpenSearchObservationConvention() {
            @Override
            public String getName() {
                return "custom.command";
            }
        });
        custom.observe(OpenSearchOperationName.GET, INDEX, () -> true);
        assertThat(stopped.getFirst().getName()).isEqualTo("custom.command");
    }
}
