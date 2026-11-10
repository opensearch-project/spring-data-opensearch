/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import io.micrometer.observation.Observation;
import java.util.function.Supplier;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;

/**
 * Observation lifecycle shared by OSC templates and their child templates.
 */
@FunctionalInterface
interface OpenSearchObservationSupport {

    Observation createObservation(OpenSearchOperationName operation, @Nullable IndexCoordinates index,
            @Nullable Integer batchSize);

    default <T> T observe(final OpenSearchOperationName operation, @Nullable final IndexCoordinates index,
            @Nullable final Integer batchSize, final Supplier<T> action) {
        return createObservation(operation, index, batchSize).observe(action);
    }
}
