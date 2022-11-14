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
import org.opensearch.data.client.junit.jupiter.OpenSearchRestTemplateConfiguration;
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * class demonstrating the setup of a JUnit 5 test in Spring Data OpenSearch that uses the rest client. The
 * ContextConfiguration must include the {@link OpenSearchRestTemplateConfiguration} class.
 */
@SpringIntegrationTest
@ContextConfiguration(classes = {OpenSearchRestTemplateConfiguration.class})
@DisplayName("a sample JUnit 5 test with rest client")
public class JUnit5SampleOpenSearchRestTemplateTests {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    @DisplayName("should have a OpenSearchRestTemplate")
    void shouldHaveARestTemplate() {
        assertThat(elasticsearchOperations).isNotNull().isInstanceOf(OpenSearchRestTemplate.class);
    }
}
