/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * Reactive observation lifecycle shared by reactive OSC templates and their child templates.
 */
@FunctionalInterface
interface ReactiveOpenSearchObservationSupport extends OpenSearchObservationSupport {

    default <T> Mono<T> observeMono(final OpenSearchOperationName operation, @Nullable final IndexCoordinates index,
            @Nullable final Integer batchSize, final Mono<T> mono) {
        return Mono.deferContextual(contextView -> {
            final var observation = createObservation(operation, index, batchSize);
            setParent(observation, contextView);
            observation.start();
            return mono.doOnError(observation::error)
                    .doFinally(signalType -> observation.stop())
                    .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
        });
    }

    default <T> Flux<T> observeFlux(final OpenSearchOperationName operation, @Nullable final IndexCoordinates index,
            @Nullable final Integer batchSize, final Flux<T> flux) {
        return Flux.deferContextual(contextView -> {
            final var observation = createObservation(operation, index, batchSize);
            setParent(observation, contextView);
            observation.start();
            return flux.doOnError(observation::error)
                    .doFinally(signalType -> observation.stop())
                    .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
        });
    }

    private static void setParent(final Observation observation, final ContextView contextView) {
        final Observation parent = contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null);
        if (parent != null) {
            observation.parentObservation(parent);
        }
    }
}
