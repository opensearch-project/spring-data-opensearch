/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import java.util.ArrayList;
import java.util.List;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.util.BinaryData;
import org.springframework.data.elasticsearch.core.query.BulkOptions;

/**
 * Batches bulk operations by count and serialized size, including metadata and line separators.
 * An operation larger than the byte limit is sent alone.
 *
 * @since 3.2
 */
final class BulkRequestSplitter {

    private final JsonpMapper jsonpMapper;

    BulkRequestSplitter(final JsonpMapper jsonpMapper) {
        this.jsonpMapper = jsonpMapper;
    }

    /**
     * Preserves operation order and uses template limits when a call does not set them.
     * Size is measured only when a byte limit is set. Empty input produces one empty batch.
     *
     * @param operations the converted client operations
     * @param options the options for this call
     * @param defaults the template defaults
     * @return the batches, never empty
     */
    List<List<BulkOperation>> split(final List<BulkOperation> operations, final BulkOptions options,
            final BulkOptions defaults) {

        final var maxOperations = options.getMaxOperations() != null
                ? options.getMaxOperations() : defaults.getMaxOperations();
        final var maxSize = options.getMaxSize() != null ? options.getMaxSize() : defaults.getMaxSize();
        if (maxOperations == null && maxSize == null) {
            return List.of(operations);
        }

        final var batches = new ArrayList<List<BulkOperation>>();
        var batch = new ArrayList<BulkOperation>();
        var batchSize = 0L;
        for (final var operation : operations) {
            final var operationSize = maxSize != null ? size(operation) : 0L;
            if (!batch.isEmpty() && ((maxOperations != null && batch.size() >= maxOperations)
                    || (maxSize != null && operationSize > maxSize - batchSize))) {
                batches.add(batch);
                batch = new ArrayList<>();
                batchSize = 0L;
            }
            batch.add(operation);
            batchSize += operationSize;
        }
        if (!batch.isEmpty() || batches.isEmpty()) {
            batches.add(batch);
        }
        return batches;
    }

    private long size(final BulkOperation operation) {
        var size = 0L;
        final var lines = operation._serializables();
        while (lines.hasNext()) {
            size += BinaryData.of(lines.next(), jsonpMapper).size() + 1;
        }
        return size;
    }
}
