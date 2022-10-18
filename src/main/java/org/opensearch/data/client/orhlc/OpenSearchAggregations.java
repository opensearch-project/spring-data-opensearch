/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;


import org.opensearch.search.aggregations.Aggregations;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.lang.NonNull;

/**
 * AggregationsContainer implementation for the OpenSearch aggregations.
 * @since 0.1
 */
public class OpenSearchAggregations implements AggregationsContainer<Aggregations> {

    private final Aggregations aggregations;

    public OpenSearchAggregations(Aggregations aggregations) {
        this.aggregations = aggregations;
    }

    @NonNull
    @Override
    public Aggregations aggregations() {
        return aggregations;
    }
}
