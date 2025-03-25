/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.testcontainers.service.connection;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("integration-test")
@SpringJUnitConfig
@Testcontainers
class OpenSearchContainerConnectionDetailsFactoryIntegrationTests {

    @Container
    @ServiceConnection
    static final OpensearchContainer<?> opensearch = new OpensearchContainer<>("opensearchproject/opensearch:2.19.1")
            .withStartupAttempts(5)
            .withStartupTimeout(Duration.ofMinutes(10));

    @Autowired
    private RestClient restClient;

    @Test
    void restClientOpensearchNodeVersion() throws IOException {
        final Request request = new Request("GET", "/");
        final Response response = restClient.performRequest(request);
        try (InputStream input = response.getEntity().getContent()) {
            JsonNode result = new ObjectMapper().readTree(input);
            assertThat(result.path("version").path("number").asText())
                    .isEqualTo("2.19.1");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(OpenSearchRestClientAutoConfiguration.class)
    static class TestConfiguration {

    }
}
