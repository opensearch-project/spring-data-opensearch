/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.data;

import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.stereotype.Component;

@Component
class OpenSearchHealthIndicator extends AbstractHealthIndicator {

    private final OpenSearchRestTemplate template;

    OpenSearchHealthIndicator(OpenSearchRestTemplate template) {
        this.template = template;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        processResponse(builder, template.cluster().health());
    }

    private void processResponse(Health.Builder builder, ClusterHealth response) {
        if (response.isTimedOut()) {
            builder.down().build();
            return;
        }

        String status = response.getStatus();
        builder.status("RED".equals(status) ? Status.OUT_OF_SERVICE : Status.UP);
        builder.withDetail("cluster_name", response.getClusterName());
        builder.withDetail("status", response.getStatus());
        builder.withDetail("timed_out", response.isTimedOut());
        builder.withDetail("number_of_nodes", response.getNumberOfNodes());
        builder.withDetail("number_of_data_nodes", response.getNumberOfDataNodes());
        builder.withDetail("active_primary_shards", response.getActivePrimaryShards());
        builder.withDetail("active_shards", response.getActiveShards());
        builder.withDetail("relocating_shards", response.getRelocatingShards());
        builder.withDetail("initializing_shards", response.getInitializingShards());
        builder.withDetail("unassigned_shards", response.getUnassignedShards());
        builder.withDetail("delayed_unassigned_shards", response.getDelayedUnassignedShards());
        builder.withDetail("number_of_pending_tasks", response.getNumberOfPendingTasks());
        builder.withDetail("number_of_in_flight_fetch", response.getNumberOfInFlightFetch());
        builder.withDetail("task_max_waiting_in_queue_millis", response.getTaskMaxWaitingTimeMillis());
        builder.withDetail("active_shards_percent_as_number", response.getActiveShardsPercent());
        builder.build();
    }
}

