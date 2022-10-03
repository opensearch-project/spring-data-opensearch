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
 * AggregationsContainer implementation for the Opensearch aggregations.
 * @since 5.0
 */
public class OpensearchAggregations implements AggregationsContainer<Aggregations> {

    private final Aggregations aggregations;

    public OpensearchAggregations(Aggregations aggregations) {
        this.aggregations = aggregations;
    }

    @NonNull
    @Override
    public Aggregations aggregations() {
        return aggregations;
    }
}
