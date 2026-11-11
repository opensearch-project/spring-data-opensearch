/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.osc;

import org.opensearch.client.transport.OpenSearchTransport;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.cluster.ReactiveClusterOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import reactor.core.publisher.Mono;

/**
 * Reactive cluster template
 */
public class ReactiveClusterTemplate
        extends ReactiveChildTemplate<OpenSearchTransport, ReactiveOpenSearchClusterClient>
        implements ReactiveClusterOperations {

    public ReactiveClusterTemplate(ReactiveOpenSearchClusterClient client,
            ElasticsearchConverter elasticsearchConverter) {
        super(client, elasticsearchConverter);
    }

    @Override
    public Mono<ClusterHealth> health() {

        return observations().mono(OpenSearchOperationName.CLUSTER_HEALTH, null, () -> {
            final var healthRequest = requestConverter.clusterHealthRequest();
            final var healthResponse = Mono.from(execute(client -> client.health(healthRequest)));
            return healthResponse.map(responseConverter::clusterHealth);
        });
    }

}
