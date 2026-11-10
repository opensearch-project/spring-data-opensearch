/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import io.micrometer.observation.Observation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;

/**
 * Context for one OpenSearch template operation.
 */
public class OpenSearchObservationContext extends Observation.Context {
    private final OpenSearchOperationName operationName;
    @Nullable private final IndexCoordinates indexCoordinates;
    @Nullable private Integer batchSize;

    public OpenSearchObservationContext(final OpenSearchOperationName operationName,
            @Nullable final IndexCoordinates indexCoordinates) {
        this.operationName = operationName;
        this.indexCoordinates = indexCoordinates;
    }

    public OpenSearchOperationName getOperationName() {
        return operationName;
    }

    @Nullable
    public IndexCoordinates getIndexCoordinates() {
        return indexCoordinates;
    }

    @Nullable
    public String getIndexName() {
        return indexCoordinates == null ? null : String.join(",", indexCoordinates.getIndexNames());
    }

    @Nullable
    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(@Nullable final Integer batchSize) {
        this.batchSize = batchSize;
    }
}
