/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Register an implementation as a bean to customize OpenSearch observations.
 */
public interface OpenSearchObservationConvention extends ObservationConvention<OpenSearchObservationContext> {
    @Override
    default boolean supportsContext(final Observation.Context context) {
        return context instanceof OpenSearchObservationContext;
    }
}
