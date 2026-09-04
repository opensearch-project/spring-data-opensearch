/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;

class OpenSearchHealthIndicatorTests {

    @Test
    void shouldReportDownWhenClusterHealthTimesOut() {
        OpenSearchHealthIndicator indicator = new OpenSearchHealthIndicator(templateWithResponse(mockHealth("GREEN", true)));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).isEmpty();
    }

    @Test
    void shouldReportOutOfServiceForRedClusterStatusAndExposeExactDetailKeys() {
        OpenSearchHealthIndicator indicator = new OpenSearchHealthIndicator(templateWithResponse(mockHealth("RED", false)));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails())
                .containsOnlyKeys(
                        "cluster_name",
                        "status",
                        "timed_out",
                        "number_of_nodes",
                        "number_of_data_nodes",
                        "active_primary_shards",
                        "active_shards",
                        "relocating_shards",
                        "initializing_shards",
                        "unassigned_shards",
                        "delayed_unassigned_shards",
                        "number_of_pending_tasks",
                        "number_of_in_flight_fetch",
                        "task_max_waiting_in_queue_millis",
                        "active_shards_percent_as_number");
        assertThat(health.getDetails().get("status")).isEqualTo("RED");
    }

    @Test
    void shouldReportUpForNonRedClusterStatus() {
        OpenSearchHealthIndicator indicator = new OpenSearchHealthIndicator(templateWithResponse(mockHealth("YELLOW", false)));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("status")).isEqualTo("YELLOW");
    }

    private OpenSearchRestTemplate templateWithResponse(ClusterHealth clusterHealth) {
        OpenSearchRestTemplate template = mock(OpenSearchRestTemplate.class);
        ClusterOperations clusterOperations = mock(ClusterOperations.class);
        when(template.cluster()).thenReturn(clusterOperations);
        when(clusterOperations.health()).thenReturn(clusterHealth);
        return template;
    }

    private ClusterHealth mockHealth(String status, boolean timedOut) {
        ClusterHealth clusterHealth = mock(ClusterHealth.class);
        when(clusterHealth.isTimedOut()).thenReturn(timedOut);
        when(clusterHealth.getStatus()).thenReturn(status);
        when(clusterHealth.getClusterName()).thenReturn("cluster");
        when(clusterHealth.getNumberOfNodes()).thenReturn(3);
        when(clusterHealth.getNumberOfDataNodes()).thenReturn(2);
        when(clusterHealth.getActivePrimaryShards()).thenReturn(10);
        when(clusterHealth.getActiveShards()).thenReturn(20);
        when(clusterHealth.getRelocatingShards()).thenReturn(1);
        when(clusterHealth.getInitializingShards()).thenReturn(0);
        when(clusterHealth.getUnassignedShards()).thenReturn(0);
        when(clusterHealth.getDelayedUnassignedShards()).thenReturn(0);
        when(clusterHealth.getNumberOfPendingTasks()).thenReturn(0);
        when(clusterHealth.getNumberOfInFlightFetch()).thenReturn(0);
        when(clusterHealth.getTaskMaxWaitingTimeMillis()).thenReturn(0L);
        when(clusterHealth.getActiveShardsPercent()).thenReturn(100.0d);
        return clusterHealth;
    }
}

