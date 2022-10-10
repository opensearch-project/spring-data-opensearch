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
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.opensearch.data.client.orhlc.ClientConfiguration.ClientConfigurationBuilderWithRequiredEndpoint;
import org.opensearch.data.client.orhlc.ClientConfiguration.MaybeSecureClientConfigurationBuilder;
import org.opensearch.data.client.orhlc.ClientConfiguration.TerminalClientConfigurationBuilder;
import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.InetSocketAddressParser;
import org.springframework.data.elasticsearch.support.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default builder implementation for {@link ClientConfiguration}.
 * @since 5.0
 */
class ClientConfigurationBuilder
        implements ClientConfigurationBuilderWithRequiredEndpoint, MaybeSecureClientConfigurationBuilder {

    private final List<InetSocketAddress> hosts = new ArrayList<>();
    private HttpHeaders headers = new HttpHeaders();
    private boolean useSsl;
    private @Nullable SSLContext sslContext;
    private @Nullable HostnameVerifier hostnameVerifier;
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration soTimeout = Duration.ofSeconds(5);
    private @Nullable String username;
    private @Nullable String password;
    private @Nullable String pathPrefix;
    private @Nullable String proxy;
    private Supplier<HttpHeaders> headersSupplier = () -> new HttpHeaders();
    private List<ClientConfiguration.ClientConfigurationCallback<?>> clientConfigurers = new ArrayList<>();

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.orhcl.ClientConfiguration.ClientConfigurationBuilderWithRequiredEndpoint#connectedTo(java.lang.String[])
     */
    @Override
    public MaybeSecureClientConfigurationBuilder connectedTo(String... hostAndPorts) {

        Assert.notEmpty(hostAndPorts, "At least one host is required");

        this.hosts.addAll(Arrays.stream(hostAndPorts)
                .map(ClientConfigurationBuilder::parse)
                .collect(Collectors.toList()));
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.orhcl.ClientConfiguration.ClientConfigurationBuilderWithRequiredEndpoint#connectedTo(java.net.InetSocketAddress[])
     */
    @Override
    public MaybeSecureClientConfigurationBuilder connectedTo(InetSocketAddress... endpoints) {

        Assert.notEmpty(endpoints, "At least one endpoint is required");

        this.hosts.addAll(Arrays.asList(endpoints));

        return this;
    }

    @Override
    public MaybeSecureClientConfigurationBuilder withProxy(String proxy) {
        Assert.hasLength(proxy, "proxy must not be null or empty");
        this.proxy = proxy;
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.orhcl.ClientConfiguration.MaybeSecureClientConfigurationBuilder#usingSsl()
     */
    @Override
    public TerminalClientConfigurationBuilder usingSsl() {

        this.useSsl = true;
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.orhcl.ClientConfiguration.MaybeSecureClientConfigurationBuilder#usingSsl(javax.net.ssl.SSLContext)
     */
    @Override
    public TerminalClientConfigurationBuilder usingSsl(SSLContext sslContext) {

        Assert.notNull(sslContext, "SSL Context must not be null");

        this.useSsl = true;
        this.sslContext = sslContext;
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.orhcl.ClientConfiguration.MaybeSecureClientConfigurationBuilder#usingSsl(javax.net.ssl.SSLContext, javax.net.ssl.HostnameVerifier)
     */
    @Override
    public TerminalClientConfigurationBuilder usingSsl(SSLContext sslContext, HostnameVerifier hostnameVerifier) {

        Assert.notNull(sslContext, "SSL Context must not be null");
        Assert.notNull(hostnameVerifier, "Host Name Verifier must not be null");

        this.useSsl = true;
        this.sslContext = sslContext;
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.orhcl.ClientConfiguration.TerminalClientConfigurationBuilder#withDefaultHeaders(org.springframework.http.HttpHeaders)
     */
    @Override
    public TerminalClientConfigurationBuilder withDefaultHeaders(HttpHeaders defaultHeaders) {

        Assert.notNull(defaultHeaders, "Default HTTP headers must not be null");

        this.headers = new HttpHeaders();
        this.headers.addAll(defaultHeaders);

        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.orhcl.ClientConfiguration.TerminalClientConfigurationBuilder#withConnectTimeout(java.time.Duration)
     */
    @Override
    public TerminalClientConfigurationBuilder withConnectTimeout(Duration timeout) {

        Assert.notNull(timeout, "I/O timeout must not be null!");

        this.connectTimeout = timeout;
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.orhcl.ClientConfiguration.TerminalClientConfigurationBuilder#withTimeout(java.time.Duration)
     */
    @Override
    public TerminalClientConfigurationBuilder withSocketTimeout(Duration timeout) {

        Assert.notNull(timeout, "Socket timeout must not be null!");

        this.soTimeout = timeout;
        return this;
    }

    @Override
    public TerminalClientConfigurationBuilder withBasicAuth(String username, String password) {

        Assert.notNull(username, "username must not be null");
        Assert.notNull(password, "password must not be null");

        this.username = username;
        this.password = password;

        return this;
    }

    @Override
    public TerminalClientConfigurationBuilder withPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;

        return this;
    }

    @Override
    public TerminalClientConfigurationBuilder withClientConfigurer(
            ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer) {

        Assert.notNull(clientConfigurer, "clientConfigurer must not be null");

        this.clientConfigurers.add(clientConfigurer);
        return this;
    }

    @Override
    public TerminalClientConfigurationBuilder withHeaders(Supplier<HttpHeaders> headers) {

        Assert.notNull(headers, "headersSupplier must not be null");

        this.headersSupplier = headers;
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.orhcl.ClientConfiguration.ClientConfigurationBuilderWithOptionalDefaultHeaders#build()
     */
    @Override
    public ClientConfiguration build() {

        if (username != null && password != null) {
            headers.setBasicAuth(username, password);
        }

        return new DefaultClientConfiguration(
                hosts,
                headers,
                useSsl,
                sslContext,
                soTimeout,
                connectTimeout,
                pathPrefix,
                hostnameVerifier,
                proxy,
                clientConfigurers,
                headersSupplier);
    }

    private static InetSocketAddress parse(String hostAndPort) {
        return InetSocketAddressParser.parse(hostAndPort, ElasticsearchHost.DEFAULT_PORT);
    }
}
