/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.docker.compose.service.connection;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(properties = {
        "spring.docker.compose.skip.in-tests=false",
        "spring.docker.compose.file=classpath:org/opensearch/spring/boot/docker/compose/service/connection/opensearch-compose.yaml",
        "spring.docker.compose.stop.command=down"
})
class OpenSearchDockerComposeConnectionDetailsFactoryTest {

    @Autowired
    private RestClient restClient;

    @Test
    void restClientOpensearchNodeVersion() throws IOException {
        final Request request = new Request("GET", "/");
        final Response response = this.restClient.performRequest(request);
        try (InputStream input = response.getEntity().getContent()) {
            JsonNode result = new ObjectMapper().readTree(input);
            assertThat(result.path("version").path("number").asText())
                    .isEqualTo("3.2.0");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(OpenSearchRestClientAutoConfiguration.class)
    static class TestConfiguration {

    }

}
