/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.util.function.Supplier;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Observation lifecycle shared by OSC templates.
 */
final class OpenSearchObservationSupport {
    private final ObservationRegistry registry;
    @Nullable private final OpenSearchObservationConvention convention;

    OpenSearchObservationSupport(final ObservationRegistry registry,
            @Nullable final OpenSearchObservationConvention convention) {
        this.registry = registry;
        this.convention = convention;
    }

    private Observation create(final OpenSearchOperationName operation, @Nullable final IndexCoordinates index,
            @Nullable final Integer batchSize) {
        return OpenSearchObservation.OPENSEARCH_COMMAND_OBSERVATION.observation(convention,
                DefaultOpenSearchObservationConvention.INSTANCE, () -> {
                    final var context = new OpenSearchObservationContext(operation, index);
                    context.setBatchSize(batchSize);
                    return context;
                }, registry);
    }

    <T> T observe(final OpenSearchOperationName operation, @Nullable final IndexCoordinates index,
            final Supplier<T> action) {
        return observe(operation, index, null, action);
    }

    <T> T observe(final OpenSearchOperationName operation, @Nullable final IndexCoordinates index,
            @Nullable final Integer batchSize, final Supplier<T> action) {
        return create(operation, index, batchSize).observe(action);
    }

    <T> Mono<T> mono(final OpenSearchOperationName operation, @Nullable final IndexCoordinates index,
            final Supplier<Mono<T>> action) {
        return flux(operation, index, null, () -> action.get().flux()).singleOrEmpty();
    }

    <T> Flux<T> flux(final OpenSearchOperationName operation, @Nullable final IndexCoordinates index,
            @Nullable final Integer batchSize, final Supplier<Flux<T>> action) {
        return Flux.deferContextual(context -> {
            final var observation = create(operation, index, batchSize);
            final Observation parent = context.getOrDefault(ObservationThreadLocalAccessor.KEY, null);
            if (parent != null) {
                observation.parentObservation(parent);
            }
            observation.start();
            return Flux.defer(action).doOnError(observation::error)
                    .doFinally(signal -> observation.stop())
                    .contextWrite(child -> child.put(ObservationThreadLocalAccessor.KEY, observation));
        });
    }
}
