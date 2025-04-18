/*
 * Copyright 2021-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.data.client.osc;

import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;
import org.opensearch.client.transport.rest_client.RestClientOptions;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.support.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility class to create the different OpenSearch clients
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@SuppressWarnings("unused")
public final class OpenSearchClients {
    public static final String IMPERATIVE_CLIENT = "imperative";
    public static final String REACTIVE_CLIENT = "reactive";

    /**
     * Name of whose value can be used to correlate log messages for this request.
     */
    private static final String X_SPRING_DATA_OPENSEARCH_CLIENT = "X-SpringDataOpenSearch-Client";
    private static final JsonpMapper DEFAULT_JSONP_MAPPER = new JacksonJsonpMapper();


    // region imperative client
    /**
     * Creates a new imperative {@link OpenSearchClient}
     *
     * @param clientConfiguration configuration options, must not be {@literal null}.
     * @return the {@link OpenSearchClient}
     */
    public static OpenSearchClient createImperative(ClientConfiguration clientConfiguration) {
        return createImperative(getRestClient(clientConfiguration), null, DEFAULT_JSONP_MAPPER);
    }

    /**
     * Creates a new imperative {@link OpenSearchClient}
     *
     * @param clientConfiguration configuration options, must not be {@literal null}.
     * @param transportOptions options to be added to each request.
     * @return the {@link OpenSearchClient}
     */
    public static OpenSearchClient createImperative(ClientConfiguration clientConfiguration,
            TransportOptions transportOptions) {
        return createImperative(getRestClient(clientConfiguration), transportOptions, DEFAULT_JSONP_MAPPER);
    }

    /**
     * Creates a new imperative {@link OpenSearchClient}
     *
     * @param restClient the RestClient to use
     * @return the {@link OpenSearchClient}
     */
    public static OpenSearchClient createImperative(RestClient restClient) {
        return createImperative(restClient, null, DEFAULT_JSONP_MAPPER);
    }

    /**
     * Creates a new imperative {@link OpenSearchClient}
     *
     * @param restClient the RestClient to use
     * @param transportOptions options to be added to each request.
     * @return the {@link OpenSearchClient}
     */
    public static OpenSearchClient createImperative(RestClient restClient, @Nullable TransportOptions transportOptions) {
        return createImperative(restClient, transportOptions, DEFAULT_JSONP_MAPPER);
    }

    /**
     * Creates a new imperative {@link OpenSearchClient}
     *
     * @param restClient the RestClient to use
     * @param transportOptions options to be added to each request.
     * @param jsonpMapper the mapper for the transport to use
     * @return the {@link OpenSearchClient}
     */
    public static OpenSearchClient createImperative(RestClient restClient, @Nullable TransportOptions transportOptions,
            JsonpMapper jsonpMapper) {

        Assert.notNull(restClient, "restClient must not be null");

        OpenSearchTransport transport = getOpenSearchTransport(restClient, IMPERATIVE_CLIENT, transportOptions,
                jsonpMapper);

        return createImperative(transport);
    }

    /**
     * Creates a new {@link OpenSearchClient} that uses the given {@link OpenSearchTransport}.
     *
     * @param transport the transport to use
     * @return the {@link OpenSearchClient}
     */
    public static AutoCloseableOpenSearchClient createImperative(OpenSearchTransport transport) {

        Assert.notNull(transport, "transport must not be null");

        return new AutoCloseableOpenSearchClient(transport);
    }
    // endregion

    // region low level RestClient
    private static RestClientOptions.Builder getRestClientOptionsBuilder(@Nullable TransportOptions transportOptions) {

        if (transportOptions instanceof RestClientOptions restClientOptions) {
            return restClientOptions.toBuilder();
        }

        var builder = new RestClientOptions.Builder(RequestOptions.DEFAULT.toBuilder());

        if (transportOptions != null) {
            transportOptions.headers().forEach(header -> builder.addHeader(header.getKey(), header.getValue()));
            transportOptions.queryParameters().forEach(builder::setParameter);
            builder.onWarnings(transportOptions.onWarnings());
        }

        return builder;
    }

    /**
     * Creates a low level {@link RestClient} for the given configuration.
     *
     * @param clientConfiguration must not be {@literal null}
     * @return the {@link RestClient}
     */
    public static RestClient getRestClient(ClientConfiguration clientConfiguration) {
        return getRestClientBuilder(clientConfiguration).build();
    }

    private static RestClientBuilder getRestClientBuilder(ClientConfiguration clientConfiguration) {
        HttpHost[] httpHosts = formattedHosts(clientConfiguration.getEndpoints(), clientConfiguration.useSsl()).stream()
                .map(HttpHost::create).toArray(HttpHost[]::new);
        RestClientBuilder builder = RestClient.builder(httpHosts);

        if (clientConfiguration.getPathPrefix() != null) {
            builder.setPathPrefix(clientConfiguration.getPathPrefix());
        }

        HttpHeaders headers = clientConfiguration.getDefaultHeaders();

        if (!headers.isEmpty()) {
            builder.setDefaultHeaders(toHeaderArray(headers));
        }

        builder.setHttpClientConfigCallback(clientBuilder -> {
            if (clientConfiguration.getCaFingerprint().isPresent()) {
                clientBuilder
                        .setSSLContext(sslContextFromCaFingerprint(clientConfiguration.getCaFingerprint().get()));
            }
            clientConfiguration.getSslContext().ifPresent(clientBuilder::setSSLContext);
            clientConfiguration.getHostNameVerifier().ifPresent(clientBuilder::setSSLHostnameVerifier);
            clientBuilder.addInterceptorLast(new CustomHeaderInjector(clientConfiguration.getHeadersSupplier()));

            RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
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

            for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurer : clientConfiguration
                    .getClientConfigurers()) {
                if (clientConfigurer instanceof OpenSearchHttpClientConfigurationCallback restClientConfigurationCallback) {
                    clientBuilder = restClientConfigurationCallback.configure(clientBuilder);
                }
            }

            return clientBuilder;
        });

        for (ClientConfiguration.ClientConfigurationCallback<?> clientConfigurationCallback : clientConfiguration
                .getClientConfigurers()) {
            if (clientConfigurationCallback instanceof OpenSearchRestClientConfigurationCallback configurationCallback) {
                builder = configurationCallback.configure(builder);
            }
        }
        return builder;
    }
    // endregion

    // region OpenSearch transport
    /**
     * Creates an {@link OpenSearchTransport} that will use the given client that additionally is customized with a
     * header to contain the clientType
     *
     * @param restClient the client to use
     * @param clientType the client type to pass in each request as header
     * @param transportOptions options for the transport
     * @param jsonpMapper mapper for the transport
     * @return OpenSearchTransport
     */
    public static OpenSearchTransport getOpenSearchTransport(RestClient restClient, String clientType,
            @Nullable TransportOptions transportOptions, JsonpMapper jsonpMapper) {

        Assert.notNull(restClient, "restClient must not be null");
        Assert.notNull(clientType, "clientType must not be null");
        Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

        TransportOptions.Builder transportOptionsBuilder = transportOptions != null ? transportOptions.toBuilder()
                : new RestClientOptions(RequestOptions.DEFAULT).toBuilder();

        RestClientOptions.Builder restClientOptionsBuilder = getRestClientOptionsBuilder(transportOptions);

        // The "application/vnd.opensearch+json" would be more appropriate here but it is not supported by 1.x line,
        // using "application/json" instead.
        ContentType jsonContentType = ContentType.APPLICATION_JSON;

        Consumer<String> setHeaderIfNotPresent = header -> {
            if (restClientOptionsBuilder.build().headers().stream() //
                    .noneMatch((h) -> h.getKey().equalsIgnoreCase(header))) {
                // need to add the compatibility header, this is only done automatically when not passing in custom options.
                // code copied from RestClientTransport as it is not available outside the package
                restClientOptionsBuilder.addHeader(header, jsonContentType.toString());
            }
        };

        setHeaderIfNotPresent.accept("Content-Type");
        setHeaderIfNotPresent.accept("Accept");

        restClientOptionsBuilder.addHeader(X_SPRING_DATA_OPENSEARCH_CLIENT, clientType);

        return new RestClientTransport(restClient, jsonpMapper, restClientOptionsBuilder.build());
    }
    // endregion

    // region reactive client
    /**
     * Creates a new {@link ReactiveOpenSearchClient}
     *
     * @param clientConfiguration configuration options, must not be {@literal null}.
     * @return the {@link ReactiveOpenSearchClient}
     */
    public static ReactiveOpenSearchClient createReactive(ClientConfiguration clientConfiguration) {

        Assert.notNull(clientConfiguration, "clientConfiguration must not be null");

        return createReactive(getRestClient(clientConfiguration), null, DEFAULT_JSONP_MAPPER);
    }

    /**
     * Creates a new {@link ReactiveOpenSearchClient}
     *
     * @param clientConfiguration configuration options, must not be {@literal null}.
     * @param transportOptions options to be added to each request.
     * @return the {@link ReactiveOpenSearchClient}
     */
    public static ReactiveOpenSearchClient createReactive(ClientConfiguration clientConfiguration,
            @Nullable TransportOptions transportOptions) {

        Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");

        return createReactive(getRestClient(clientConfiguration), transportOptions, DEFAULT_JSONP_MAPPER);
    }

    /**
     * Creates a new {@link ReactiveOpenSearchClient}
     *
     * @param clientConfiguration configuration options, must not be {@literal null}.
     * @param transportOptions options to be added to each request.
     * @param jsonpMapper the JsonpMapper to use
     * @return the {@link ReactiveOpenSearchClient}
     */
    public static ReactiveOpenSearchClient createReactive(ClientConfiguration clientConfiguration,
            @Nullable TransportOptions transportOptions, JsonpMapper jsonpMapper) {

        Assert.notNull(clientConfiguration, "ClientConfiguration must not be null!");
        Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

        return createReactive(getRestClient(clientConfiguration), transportOptions, jsonpMapper);
    }

    /**
     * Creates a new {@link ReactiveOpenSearchClient}.
     *
     * @param restClient the underlying {@link RestClient}
     * @return the {@link ReactiveOpenSearchClient}
     */
    public static ReactiveOpenSearchClient createReactive(RestClient restClient) {
        return createReactive(restClient, null, DEFAULT_JSONP_MAPPER);
    }

    /**
     * Creates a new {@link ReactiveOpenSearchClient}.
     *
     * @param restClient the underlying {@link RestClient}
     * @param transportOptions options to be added to each request.
     * @return the {@link ReactiveOpenSearchClient}
     */
    public static ReactiveOpenSearchClient createReactive(RestClient restClient,
            @Nullable TransportOptions transportOptions, JsonpMapper jsonpMapper) {

        Assert.notNull(restClient, "restClient must not be null");

        var transport = getOpenSearchTransport(restClient, REACTIVE_CLIENT, transportOptions, jsonpMapper);
        return createReactive(transport);
    }

    /**
     * Creates a new {@link ReactiveOpenSearchClient} that uses the given {@link OpenSearchTransport}.
     *
     * @param transport the transport to use
     * @return the {@link ReactiveOpenSearchClient}
     */
    public static ReactiveOpenSearchClient createReactive(OpenSearchTransport transport) {

        Assert.notNull(transport, "transport must not be null");

        return new ReactiveOpenSearchClient(transport);
    }
    // endregion


    private static List<String> formattedHosts(List<InetSocketAddress> hosts, boolean useSsl) {
        return hosts.stream().map(it -> (useSsl ? "https" : "http") + "://" + it.getHostString() + ':' + it.getPort())
                .collect(Collectors.toList());
    }

    private static org.apache.http.Header[] toHeaderArray(HttpHeaders headers) {
        return headers.entrySet().stream() //
                .flatMap(entry -> entry.getValue().stream() //
                        .map(value -> new BasicHeader(entry.getKey(), value))) //
                .toArray(org.apache.http.Header[]::new);
    }

    /**
     * Interceptor to inject custom supplied headers.
     *
     * @since 4.4
     */
    private record CustomHeaderInjector(Supplier<HttpHeaders> headersSupplier) implements HttpRequestInterceptor {

        @Override
        public void process(HttpRequest request, HttpContext context) {
            HttpHeaders httpHeaders = headersSupplier.get();

            if (httpHeaders != null && !httpHeaders.isEmpty()) {
                Arrays.stream(toHeaderArray(httpHeaders)).forEach(request::addHeader);
            }
        }
    }

    /**
     * {@link org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationCallback} to configure
     * the OpenSearch RestClient's Http client with a {@link HttpAsyncClientBuilder}
     *
     * @since 4.4
     */
    public interface OpenSearchHttpClientConfigurationCallback
            extends ClientConfiguration.ClientConfigurationCallback<HttpAsyncClientBuilder> {

        static OpenSearchHttpClientConfigurationCallback from(
                Function<HttpAsyncClientBuilder, HttpAsyncClientBuilder> httpClientBuilderCallback) {

            Assert.notNull(httpClientBuilderCallback, "httpClientBuilderCallback must not be null");

            return httpClientBuilderCallback::apply;
        }
    }

    /**
     * {@link org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationCallback} to configure
     * the RestClient client with a {@link RestClientBuilder}
     *
     * @since 5.0
     */
    public interface OpenSearchRestClientConfigurationCallback
            extends ClientConfiguration.ClientConfigurationCallback<RestClientBuilder> {

        static OpenSearchRestClientConfigurationCallback from(
                Function<RestClientBuilder, RestClientBuilder> restClientBuilderCallback) {

            Assert.notNull(restClientBuilderCallback, "restClientBuilderCallback must not be null");

            return restClientBuilderCallback::apply;
        }
    }

    /**
     * Copy / paste of co.elastic.clients.transport.TransportUtils#sslContextFromCaFingerprint (licensed under ASFv2), since
     * OpenSearch Java client does not support such SSL configuration at the moment.
     */
    private static SSLContext sslContextFromCaFingerprint(String fingerPrint) {

        fingerPrint = fingerPrint.replace(":", "");
        int len = fingerPrint.length();
        byte[] fpBytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            fpBytes[i / 2] = (byte) (
                (Character.digit(fingerPrint.charAt(i), 16) << 4) +
                Character.digit(fingerPrint.charAt(i+1), 16)
            );
        }

        try {
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    throw new CertificateException("This is a client-side only trust manager");
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                    // The CA root is the last element of the chain
                    X509Certificate anchor = chain[chain.length - 1];

                    byte[] bytes;
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        md.update(anchor.getEncoded());
                        bytes = md.digest();
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }

                    if (Arrays.equals(fpBytes, bytes)) {
                        return;
                    }

                    throw new CertificateException("Untrusted certificate: " + anchor.getSubjectX500Principal());
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new X509TrustManager[] { tm }, null);
            return sslContext;

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            // Exceptions that should normally not occur
            throw new RuntimeException(e);
        }
    }

}
