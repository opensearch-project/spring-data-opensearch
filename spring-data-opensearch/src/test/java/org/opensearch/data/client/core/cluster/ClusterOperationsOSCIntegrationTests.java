/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.cluster;

import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.springframework.data.elasticsearch.core.cluster.ClusterOperationsIntegrationTests;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {OpenSearchTemplateConfiguration.class})
public class ClusterOperationsOSCIntegrationTests extends ClusterOperationsIntegrationTests {}
