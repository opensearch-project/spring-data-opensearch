/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import io.micrometer.common.KeyValues;

/**
 * Default names and tags for OpenSearch observations.
 */
public class DefaultOpenSearchObservationConvention implements OpenSearchObservationConvention {
    public static final DefaultOpenSearchObservationConvention INSTANCE = new DefaultOpenSearchObservationConvention();

    @Override
    public String getName() {
        return OpenSearchObservation.OPENSEARCH_COMMAND_OBSERVATION.getName();
    }

    @Override
    public String getContextualName(final OpenSearchObservationContext context) {
        final var index = context.getIndexName();
        return context.getOperationName().getValue() + (index == null ? "" : " " + index);
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(final OpenSearchObservationContext context) {
        return KeyValues.of(OpenSearchObservation.LowCardinalityKeyNames.OPERATION
                .withValue(context.getOperationName().getValue()));
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(final OpenSearchObservationContext context) {
        var values = KeyValues.empty();
        final var index = context.getIndexName();
        if (index != null) {
            values = values.and(OpenSearchObservation.HighCardinalityKeyNames.COLLECTION.withValue(index));
        }
        final var batchSize = context.getBatchSize();
        if (batchSize != null) {
            values = values.and(OpenSearchObservation.HighCardinalityKeyNames.BATCH_SIZE.withValue(batchSize.toString()));
        }
        return values;
    }
}
