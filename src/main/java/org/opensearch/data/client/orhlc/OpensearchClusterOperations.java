/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;


import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;
import org.springframework.util.Assert;

/**
 * Opensearch cluster operations
 * @since 5.0
 */
public class OpensearchClusterOperations {
    /**
     * Creates a ClusterOperations for a {@link OpensearchRestTemplate}.
     *
     * @param template the template, must not be {@literal null}
     * @return ClusterOperations
     */
    public static ClusterOperations forTemplate(OpensearchRestTemplate template) {

        Assert.notNull(template, "template must not be null");

        return new DefaultClusterOperations(template);
    }
}
