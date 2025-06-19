/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.RequestConfig.Builder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.data.elasticsearch.support.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Utility class for common access to OpenSearch clients. {@link RestClients} consolidates set up routines for the
 * various drivers into a single place.
 * @since 0.1
 */
public final class RestClients {
    private RestClients() {}

    /**
     * Start here to create a new client tailored to your needs.
     *
     * @return new instance of {@link OpenSearchRestClient}.
     */
    public static OpenSearchRestClient create(ClientConfiguration clientConfiguration) {

        Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");

        HttpHost[] httpHosts = formattedHosts(clientConfiguration.getEndpoints(), clientConfiguration.useSsl()).stream()
                .map(s -> {
                    try {
                        return HttpHost.create(s);
                    } catch (final URISyntaxException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .toArray(HttpHost[]::new);
        RestClientBuilder builder = RestClient.builder(httpHosts);

        if (clientConfiguration.getPathPrefix() != null) {
            builder.setPathPrefix(clientConfiguration.getPathPrefix());
        }

        HttpHeaders headers = clientConfiguration.getDefaultHeaders();

        if (!headers.isEmpty()) {
            builder.setDefaultHeaders(toHeaderArray(headers));
        }

        builder.setHttpClientConfigCallback(clientBuilder -> {
            ClientTlsStrategyBuilder tlsStrategy = ClientTlsStrategyBuilder.create();

            clientConfiguration.getSslContext().ifPresent(tlsStrategy::setSslContext);
            clientConfiguration.getHostNameVerifier().ifPresent(tlsStrategy::setHostnameVerifier);
            clientBuilder.addRequestInterceptorLast(new CustomHeaderInjector(clientConfiguration.getHeadersSupplier()));

            Builder requestConfigBuilder = RequestConfig.custom();
            Duration connectTimeout = clientConfiguration.getConnectTimeout();

            if (!connectTimeout.isNegative()) {
                requestConfigBuilder.setConnectTimeout(Timeout.ofMilliseconds(connectTimeout.toMillis()));
            }

            Duration socketTimeout = clientConfiguration.getSocketTimeout();

            if (!socketTimeout.isNegative()) {
                requestConfigBuilder.setConnectionRequestTimeout(Timeout.ofMilliseconds(socketTimeout.toMillis()));
            }

            clientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

            clientConfiguration.getProxy().map(s -> {
                try {
                    return HttpHost.create(s);
                } catch (final URISyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
            }).ifPresent(clientBuilder::setProxy);

            for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer :
                    clientConfiguration.getClientConfigurers()) {
                if (clientConfigurer instanceof RestClientConfigurationCallback) {
                    RestClientConfigurationCallback restClientConfigurationCallback =
                            (RestClientConfigurationCallback) clientConfigurer;
                    clientBuilder = restClientConfigurationCallback.configure(clientBuilder);
                }
            }

            final PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                    .setTlsStrategy(tlsStrategy.build())
                    .build();

            return clientBuilder.setConnectionManager(connectionManager);
        });

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return () -> client;
    }

    private static Header[] toHeaderArray(HttpHeaders headers) {
        return headers.entrySet().stream() //
                .flatMap(entry -> entry.getValue().stream() //
                        .map(value -> new BasicHeader(entry.getKey(), value))) //
                .toArray(Header[]::new);
    }

    private static List<String> formattedHosts(List<InetSocketAddress> hosts, boolean useSsl) {
        return hosts.stream()
                .map(it -> (useSsl ? "https" : "http") + "://" + it.getHostString() + ":" + it.getPort())
                .collect(Collectors.toList());
    }

    /**
     * @author Christoph Strobl
     */
    @FunctionalInterface
    public interface OpenSearchRestClient extends Closeable {

        /**
         * Apply the configuration to create a {@link RestHighLevelClient}.
         *
         * @return new instance of {@link RestHighLevelClient}.
         */
        RestHighLevelClient rest();

        /**
         * Apply the configuration to create a {@link RestClient}.
         *
         * @return new instance of {@link RestClient}.
         */
        default RestClient lowLevelRest() {
            return rest().getLowLevelClient();
        }

        @Override
        default void close() throws IOException {
            rest().close();
        }
    }

    /**
     * Interceptor to inject custom supplied headers.
     */
    private static class CustomHeaderInjector implements HttpRequestInterceptor {

        public CustomHeaderInjector(Supplier<HttpHeaders> headersSupplier) {
            this.headersSupplier = headersSupplier;
        }

        private final Supplier<HttpHeaders> headersSupplier;

        @Override
        public void process(HttpRequest request,  EntityDetails entity, HttpContext context) {
            HttpHeaders httpHeaders = headersSupplier.get();

            if (httpHeaders != null && !httpHeaders.isEmpty()) {
                Arrays.stream(toHeaderArray(httpHeaders)).forEach(request::addHeader);
            }
        }
    }

    /**
     * {@link org.opensearch.data.client.orhlc.ClientConfiguration.ClientConfigurationCallback} to configure
     * the RestClient with a {@link HttpAsyncClientBuilder}
     */
    public interface RestClientConfigurationCallback
            extends ClientConfiguration.ClientConfigurationCallback<HttpAsyncClientBuilder> {

        static RestClientConfigurationCallback from(
                Function<HttpAsyncClientBuilder, HttpAsyncClientBuilder> clientBuilderCallback) {

            Assert.notNull(clientBuilderCallback, "clientBuilderCallback must not be null");

            // noinspection NullableProblems
            return clientBuilderCallback::apply;
        }
    }
}
