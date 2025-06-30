/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.junit.jupiter.Tags;
import org.springframework.data.elasticsearch.support.HttpHeaders;

/**
 * Integration tests for {@link ClientConfiguration}.
 */
@Tag(Tags.INTEGRATION_TEST)
public class ClientConfigurationIntegrationTests {

    @Test // DATAES-488
    public void shouldCreateSimpleConfiguration() {

        ClientConfiguration clientConfiguration = ClientConfiguration.create("localhost:9200");

        assertThat(clientConfiguration.getEndpoints())
                .containsOnly(InetSocketAddress.createUnresolved("localhost", 9200));
    }

    @Test // DATAES-488, DATAES-504, DATAES-650, DATAES-700
    public void shouldCreateCustomizedConfiguration() {

        HttpHeaders headers = new HttpHeaders();
        headers.set("foo", "bar");

        ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
                .connectedTo("foo", "bar") //
                .usingSsl() //
                .withDefaultHeaders(headers) //
                .withConnectTimeout(Duration.ofDays(1))
                .withSocketTimeout(Duration.ofDays(2)) //
                .withPathPrefix("myPathPrefix") //
                .withProxy("localhost:8080")
                .build();

        assertThat(clientConfiguration.getEndpoints())
                .containsOnly(
                        InetSocketAddress.createUnresolved("foo", 9200),
                        InetSocketAddress.createUnresolved("bar", 9200));
        assertThat(clientConfiguration.useSsl()).isTrue();
        assertThat(clientConfiguration.getDefaultHeaders().get("foo")).containsOnly("bar");
        assertThat(clientConfiguration.getConnectTimeout()).isEqualTo(Duration.ofDays(1));
        assertThat(clientConfiguration.getSocketTimeout()).isEqualTo(Duration.ofDays(2));
        assertThat(clientConfiguration.getPathPrefix()).isEqualTo("myPathPrefix");
        assertThat(clientConfiguration.getProxy()).contains("localhost:8080");
    }

    @Test // DATAES-488, DATAES-504
    public void shouldCreateSslConfiguration() {

        SSLContext sslContext = mock(SSLContext.class);

        ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
                .connectedTo("foo", "bar") //
                .usingSsl(sslContext) //
                .build();

        assertThat(clientConfiguration.getEndpoints())
                .containsOnly(
                        InetSocketAddress.createUnresolved("foo", 9200),
                        InetSocketAddress.createUnresolved("bar", 9200));
        assertThat(clientConfiguration.useSsl()).isTrue();
        assertThat(clientConfiguration.getSslContext()).contains(sslContext);
        assertThat(clientConfiguration.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(clientConfiguration.getSocketTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test // DATAES-607
    public void shouldAddBasicAuthenticationHeaderWhenNoHeadersAreSet() {

        String username = "secretUser";
        String password = "secretPassword";

        ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
                .connectedTo("foo", "bar") //
                .withBasicAuth(username, password) //
                .build();

        assertThat(clientConfiguration.getDefaultHeaders().get(HttpHeaders.AUTHORIZATION))
                .containsOnly(buildBasicAuth(username, password));
    }

    @Test // DATAES-607
    public void shouldAddBasicAuthenticationHeaderAndKeepHeaders() {

        String username = "secretUser";
        String password = "secretPassword";

        HttpHeaders defaultHeaders = new HttpHeaders();
        defaultHeaders.set("foo", "bar");

        ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
                .connectedTo("foo", "bar") //
                .withBasicAuth(username, password) //
                .withDefaultHeaders(defaultHeaders) //
                .build();

        HttpHeaders httpHeaders = clientConfiguration.getDefaultHeaders();

        assertThat(httpHeaders.get(HttpHeaders.AUTHORIZATION)).containsOnly(buildBasicAuth(username, password));
        assertThat(httpHeaders.getFirst("foo")).isEqualTo("bar");
        assertThat(defaultHeaders.get(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test // DATAES-673
    public void shouldCreateSslConfigurationWithHostnameVerifier() {

        SSLContext sslContext = mock(SSLContext.class);

        ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
                .connectedTo("foo", "bar") //
                .usingSsl(sslContext, NoopHostnameVerifier.INSTANCE) //
                .build();

        assertThat(clientConfiguration.getEndpoints())
                .containsOnly(
                        InetSocketAddress.createUnresolved("foo", 9200),
                        InetSocketAddress.createUnresolved("bar", 9200));
        assertThat(clientConfiguration.useSsl()).isTrue();
        assertThat(clientConfiguration.getSslContext()).contains(sslContext);
        assertThat(clientConfiguration.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(clientConfiguration.getSocketTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(clientConfiguration.getHostNameVerifier()).contains(NoopHostnameVerifier.INSTANCE);
    }

    @SuppressWarnings("unchecked")
    @Test // #1885
    @DisplayName("should use configured client configurer")
    void shouldUseConfiguredClientConfigurer() {

        AtomicInteger callCounter = new AtomicInteger();

        ClientConfiguration clientConfiguration = ClientConfiguration.builder() //
                .connectedTo("foo", "bar") //
                .withClientConfigurer(clientConfigurer -> {
                    callCounter.incrementAndGet();
                    return clientConfigurer;
                }) //
                .build();

        ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer =
                clientConfiguration.getClientConfigurers().get(0);

        ((ClientConfiguration.ClientConfigurationCallback<Object>) clientConfigurer).configure(new Object());
        assertThat(callCounter.get()).isEqualTo(1);
    }

    private static String buildBasicAuth(String username, String password) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        return Objects.requireNonNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
    }
}
