/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for {@link OpenSearchRestClientAutoConfiguration}.
 *
 * Adaptation of the {@link org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfigurationIntegrationTests} to
 * the needs of OpenSearch.
 */
@Testcontainers(disabledWithoutDocker = true)
class OpenSearchRestClientAutoConfigurationIntegrationTests extends AbstractOpenSearchIntegrationTest {
    @Container
    static final OpenSearchContainer<?> opensearch = new OpenSearchContainer<>(getDockerImageName())
            .withStartupAttempts(5)
            .withStartupTimeout(Duration.ofMinutes(10));

    @Container
    static final OpenSearchContainer<?> secureOpensearch = new OpenSearchContainer<>(getDockerImageName())
            .withSecurityEnabled()
            .withStartupAttempts(5)
            .withStartupTimeout(Duration.ofMinutes(10));

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenSearchRestClientAutoConfiguration.class));

    @Test
    void restClientOpensearchNodeVersion() {
        this.contextRunner
                .withPropertyValues(
                        "opensearch.uris=" + opensearch.getHttpHostAddress(),
                        "opensearch.connection-timeout=120s",
                        "opensearch.socket-timeout=120s")
                .run((context) -> {
                    final RestClient client = context.getBean(RestClient.class);
                    final Request request = new Request("GET", "/");
                    final Response response = client.performRequest(request);
                    try (InputStream input = response.getEntity().getContent()) {
                        JsonNode result = new ObjectMapper().readTree(input);
                        assertThat(result.path("version").path("number").asText())
                                .isEqualTo(getOpenSearchVersion());
                    }
                });
    }

    @Test
    void restClientCanQueryOpensearchNode() {
        this.contextRunner
                .withPropertyValues(
                        "opensearch.uris=" + opensearch.getHttpHostAddress(),
                        "opensearch.connection-timeout=120s",
                        "opensearch.socket-timeout=120s")
                .run((context) -> {
                    final RestClient client = context.getBean(RestClient.class);
                    Request index = new Request("PUT", "/test/_doc/2");
                    index.setJsonEntity("{" + "  \"a\": \"alpha\"," + "  \"b\": \"bravo\"" + "}");
                    client.performRequest(index);
                    final Request request = new Request("GET", "/test/_doc/2");
                    final Response response = client.performRequest(request);
                    try (InputStream input = response.getEntity().getContent()) {
                        JsonNode result = new ObjectMapper().readTree(input);
                        assertThat(result.path("found").asBoolean()).isTrue();
                    }
                });
    }

    @Test
    void restClientWithSslCanConnectToOpensearch() {
        this.contextRunner
                .withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
                .withPropertyValues(
                        "opensearch.uris=" + secureOpensearch.getHttpHostAddress(),
                        "opensearch.connection-timeout=120s",
                        "opensearch.socket-timeout=120s",
                        "opensearch.username=" + secureOpensearch.getUsername(),
                        "opensearch.password=" + secureOpensearch.getPassword(),
                        "opensearch.restclient.ssl.bundle=opensearch-demo-ca",
                        "spring.ssl.bundle.pem.opensearch-demo-ca.truststore.certificate=classpath:opensearch-demo-ca.pem"
                )
                .run((context) -> {
                    final RestClient client = context.getBean(RestClient.class);
                    final Request request = new Request("GET", "/");

                    final Response response = client.performRequest(request);

                    assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
                });
    }
}
