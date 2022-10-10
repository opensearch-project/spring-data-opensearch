/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.config;


import org.opensearch.data.client.junit.jupiter.OpensearchRestTemplateConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.config.AuditingIntegrationTests;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {AuditingORHLCIntegrationTests.Config.class})
public class AuditingORHLCIntegrationTests extends AuditingIntegrationTests {

    @Import({OpensearchRestTemplateConfiguration.class, AuditingIntegrationTests.Config.class})
    static class Config {}
}
