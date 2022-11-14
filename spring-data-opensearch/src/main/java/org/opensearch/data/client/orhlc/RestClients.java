/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.data.elasticsearch.client.ClientLogger;
import org.springframework.data.elasticsearch.support.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Utility class for common access to OpenSearch clients. {@link RestClients} consolidates set up routines for the
 * various drivers into a single place.
 * @since 0.1
 */
public final class RestClients {

    /**
     * Name of whose value can be used to correlate log messages for this request.
     */
    private static final String LOG_ID_ATTRIBUTE = RestClients.class.getName() + ".LOG_ID";

    private RestClients() {}

    /**
     * Start here to create a new client tailored to your needs.
     *
     * @return new instance of {@link OpenSearchRestClient}.
     */
    public static OpenSearchRestClient create(ClientConfiguration clientConfiguration) {

        Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");

        HttpHost[] httpHosts = formattedHosts(clientConfiguration.getEndpoints(), clientConfiguration.useSsl()).stream()
                .map(HttpHost::create)
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
            clientConfiguration.getSslContext().ifPresent(clientBuilder::setSSLContext);
            clientConfiguration.getHostNameVerifier().ifPresent(clientBuilder::setSSLHostnameVerifier);
            clientBuilder.addInterceptorLast(new CustomHeaderInjector(clientConfiguration.getHeadersSupplier()));

            if (ClientLogger.isEnabled()) {
                HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();

                clientBuilder.addInterceptorLast((HttpRequestInterceptor) interceptor);
                clientBuilder.addInterceptorLast((HttpResponseInterceptor) interceptor);
            }

            Builder requestConfigBuilder = RequestConfig.custom();
            Duration connectTimeout = clientConfiguration.getConnectTimeout();

            if (!connectTimeout.isNegative()) {
                requestConfigBuilder.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
            }

            Duration socketTimeout = clientConfiguration.getSocketTimeout();

            if (!socketTimeout.isNegative()) {
                requestConfigBuilder.setSocketTimeout(Math.toIntExact(socketTimeout.toMillis()));
                requestConfigBuilder.setConnectionRequestTimeout(Math.toIntExact(socketTimeout.toMillis()));
            }

            clientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

            clientConfiguration.getProxy().map(HttpHost::create).ifPresent(clientBuilder::setProxy);

            for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer :
                    clientConfiguration.getClientConfigurers()) {
                if (clientConfigurer instanceof RestClientConfigurationCallback) {
                    RestClientConfigurationCallback restClientConfigurationCallback =
                            (RestClientConfigurationCallback) clientConfigurer;
                    clientBuilder = restClientConfigurationCallback.configure(clientBuilder);
                }
            }

            return clientBuilder;
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
     * Logging interceptors for OpenSearch client logging.
     *
     * @see ClientLogger
     */
    private static class HttpLoggingInterceptor implements HttpResponseInterceptor, HttpRequestInterceptor {

        @Override
        public void process(HttpRequest request, HttpContext context) throws IOException {

            String logId = (String) context.getAttribute(RestClients.LOG_ID_ATTRIBUTE);

            if (logId == null) {
                logId = ClientLogger.newLogId();
                context.setAttribute(RestClients.LOG_ID_ATTRIBUTE, logId);
            }

            if (request instanceof HttpEntityEnclosingRequest
                    && ((HttpEntityEnclosingRequest) request).getEntity() != null) {

                HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                entity.writeTo(buffer);

                if (!entity.isRepeatable()) {
                    entityRequest.setEntity(new ByteArrayEntity(buffer.toByteArray()));
                }

                ClientLogger.logRequest(
                        logId,
                        request.getRequestLine().getMethod(),
                        request.getRequestLine().getUri(),
                        "",
                        buffer::toString);
            } else {
                ClientLogger.logRequest(
                        logId,
                        request.getRequestLine().getMethod(),
                        request.getRequestLine().getUri(),
                        "");
            }
        }

        @Override
        public void process(HttpResponse response, HttpContext context) {
            String logId = (String) context.getAttribute(RestClients.LOG_ID_ATTRIBUTE);
            ClientLogger.logRawResponse(logId, response.getStatusLine().getStatusCode());
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
        public void process(HttpRequest request, HttpContext context) {
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
