/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.junit.jupiter.ReactiveOpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * class demonstrating the setup of a reactive JUnit 5 test in Spring Data Elasticsearch that uses the new rest client.
 * The ContextConfiguration must include the {@link ReactiveOpenSearchTemplateConfiguration} class.
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { ReactiveOpenSearchTemplateConfiguration.class })
@DisplayName("a sample reactive JUnit 5 test with the new rest client")
public class JUnit5SampleReactiveOSCTests {

    @Autowired private ReactiveElasticsearchOperations elasticsearchOperations;

    @Test
    @DisplayName("should have an ReactiveOpenSearchTemplate")
    void shouldHaveAElasticsearchTemplate() {
        assertThat(elasticsearchOperations).isNotNull().isInstanceOf(ReactiveOpenSearchTemplate.class);
    }
}
