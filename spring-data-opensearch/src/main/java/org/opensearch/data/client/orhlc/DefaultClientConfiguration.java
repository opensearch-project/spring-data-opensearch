/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.springframework.data.elasticsearch.support.HttpHeaders;
import org.springframework.lang.Nullable;

/**
 * Default {@link ClientConfiguration} implementation.
 * @since 0.1
 */
class DefaultClientConfiguration implements ClientConfiguration {

    private final List<InetSocketAddress> hosts;
    private final HttpHeaders headers;
    private final boolean useSsl;
    private final @Nullable SSLContext sslContext;
    private final Duration soTimeout;
    private final Duration connectTimeout;
    private final @Nullable String pathPrefix;
    private final @Nullable HostnameVerifier hostnameVerifier;
    private final @Nullable String proxy;
    private final Supplier<HttpHeaders> headersSupplier;
    private final List<ClientConfigurationCallback<?>> clientConfigurers;

    DefaultClientConfiguration(
            List<InetSocketAddress> hosts,
            HttpHeaders headers,
            boolean useSsl,
            @Nullable SSLContext sslContext,
            Duration soTimeout,
            Duration connectTimeout,
            @Nullable String pathPrefix,
            @Nullable HostnameVerifier hostnameVerifier,
            @Nullable String proxy,
            List<ClientConfigurationCallback<?>> clientConfigurers,
            Supplier<HttpHeaders> headersSupplier) {

        this.hosts = Collections.unmodifiableList(new ArrayList<>(hosts));
        this.headers = headers;
        this.useSsl = useSsl;
        this.sslContext = sslContext;
        this.soTimeout = soTimeout;
        this.connectTimeout = connectTimeout;
        this.pathPrefix = pathPrefix;
        this.hostnameVerifier = hostnameVerifier;
        this.proxy = proxy;
        this.clientConfigurers = clientConfigurers;
        this.headersSupplier = headersSupplier;
    }

    @Override
    public List<InetSocketAddress> getEndpoints() {
        return this.hosts;
    }

    @Override
    public HttpHeaders getDefaultHeaders() {
        return this.headers;
    }

    @Override
    public boolean useSsl() {
        return this.useSsl;
    }

    @Override
    public Optional<SSLContext> getSslContext() {
        return Optional.ofNullable(this.sslContext);
    }

    @Override
    public Optional<HostnameVerifier> getHostNameVerifier() {
        return Optional.ofNullable(this.hostnameVerifier);
    }

    @Override
    public Duration getConnectTimeout() {
        return this.connectTimeout;
    }

    @Override
    public Duration getSocketTimeout() {
        return this.soTimeout;
    }

    @Nullable
    @Override
    public String getPathPrefix() {
        return this.pathPrefix;
    }

    @Override
    public Optional<String> getProxy() {
        return Optional.ofNullable(proxy);
    }

    @Override
    public <T> List<ClientConfigurationCallback<?>> getClientConfigurers() {
        return clientConfigurers;
    }

    @Override
    public Supplier<HttpHeaders> getHeadersSupplier() {
        return headersSupplier;
    }
}
