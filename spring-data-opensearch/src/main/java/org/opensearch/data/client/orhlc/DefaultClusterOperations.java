/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.client.RequestOptions;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;

/**
 * Default implementation of {@link ClusterOperations} using the {@link OpenSearchRestTemplate}.
 * @since 0.1
 */
class DefaultClusterOperations implements ClusterOperations {

    private final OpenSearchRestTemplate template;

    DefaultClusterOperations(OpenSearchRestTemplate template) {
        this.template = template;
    }

    @Override
    public ClusterHealth health() {

        ClusterHealthResponse clusterHealthResponse =
                template.execute(client -> client.cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT));
        return ResponseConverter.clusterHealth(clusterHealthResponse);
    }
}
