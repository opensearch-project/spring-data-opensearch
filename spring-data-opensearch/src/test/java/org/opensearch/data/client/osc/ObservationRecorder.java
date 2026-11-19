/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class ObservationRecorder implements ObservationHandler<OpenSearchObservationContext> {
    final ObservationRegistry registry = ObservationRegistry.create();
    final List<OpenSearchObservationContext> started = new CopyOnWriteArrayList<>();
    final List<OpenSearchObservationContext> stopped = new CopyOnWriteArrayList<>();

    ObservationRecorder() {
        registry.observationConfig().observationHandler(this);
    }

    @Override
    public boolean supportsContext(final Observation.Context context) {
        return context instanceof OpenSearchObservationContext;
    }

    @Override
    public void onStart(final OpenSearchObservationContext context) {
        started.add(context);
    }

    @Override
    public void onStop(final OpenSearchObservationContext context) {
        stopped.add(context);
    }

    void assertOperations(final OpenSearchOperationName... operations) {
        assertThat(started).extracting(OpenSearchObservationContext::getOperationName).containsExactly(operations);
        assertThat(stopped).hasSameSizeAs(started).containsAll(started);
    }

    void clear() {
        started.clear();
        stopped.clear();
    }
}
