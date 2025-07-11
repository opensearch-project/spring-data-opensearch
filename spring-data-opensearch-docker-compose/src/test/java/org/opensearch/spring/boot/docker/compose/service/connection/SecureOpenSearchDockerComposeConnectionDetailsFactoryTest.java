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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.junit.jupiter.api.Test;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.RestClientBuilderCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(properties = {
        "spring.docker.compose.skip.in-tests=false",
        "spring.docker.compose.file=classpath:org/opensearch/spring/boot/docker/compose/service/connection/secure-opensearch-compose.yaml",
        "spring.docker.compose.stop.command=down"
})
class SecureOpenSearchDockerComposeConnectionDetailsFactoryTest {

    @Autowired
    private RestClient restClient;

    @Test
    void restClientOpensearchNodeVersion() throws IOException {
        final Request request = new Request("GET", "/");
        final Response response = this.restClient.performRequest(request);
        try (InputStream input = response.getEntity().getContent()) {
            JsonNode result = new ObjectMapper().readTree(input);
            assertThat(result.path("version").path("number").asText())
                    .isEqualTo("3.1.0");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(OpenSearchRestClientAutoConfiguration.class)
    static class TestConfiguration {

        @Bean
        RestClientBuilderCustomizer customizer() {
            return new RestClientBuilderCustomizer() {

                @Override
                public void customize(RestClientBuilder builder) {

                }

                @Override
                public void customize(HttpAsyncClientBuilder builder) {
                    final SSLContext sslcontext;
                    try {
                        sslcontext = SSLContextBuilder.create()
                                .loadTrustMaterial(null, new TrustAllStrategy())
                                .build();
                    } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                        throw new RuntimeException(e);
                    }


                    final ClientTlsStrategyBuilder tlsStrategy = ClientTlsStrategyBuilder.create()
                            .setSslContext(sslcontext);

                    final PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder
                            .create()
                            .setTlsStrategy(tlsStrategy.build())
                            .build();

                    builder.setConnectionManager(connectionManager);
                }
            };
        }

    }

}
