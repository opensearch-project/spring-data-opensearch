/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.*;
import static io.specto.hoverfly.junit.verification.HoverflyVerifications.*;
import static org.assertj.core.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
import io.specto.hoverfly.junit5.api.HoverflyCapture;
import io.specto.hoverfly.junit5.api.HoverflyConfig;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.data.elasticsearch.junit.jupiter.Tags;
import org.springframework.data.elasticsearch.support.HttpHeaders;

/**
 * We need hoverfly for testing the reactive code to use a proxy. Wiremock cannot intercept the proxy calls as WebClient
 * uses HTTP CONNECT on proxy requests which wiremock does not support.
 */
@Tag(Tags.INTEGRATION_TEST)
@HoverflyCapture(path = "target/hoverfly", config = @HoverflyConfig(proxyLocalHost = true, plainHttpTunneling = true))
@ExtendWith(HoverflyExtension.class)
public class RestClientsIntegrationTests {

    @ParameterizedTest(name = "{0}") // DATAES-700
    @MethodSource("clientUnderTestFactorySource")
    @DisplayName("should use configured proxy")
    void shouldUseConfiguredProxy(ClientUnderTestFactory clientUnderTestFactory, Hoverfly hoverfly) throws IOException {

        wireMockServer(server -> {

            // wiremock is the dummy server, hoverfly the proxy

            String serviceHost = "localhost:" + server.port();
            String proxyHost = "localhost:" + hoverfly.getHoverflyConfig().getProxyPort();

            ClientConfigurationBuilder configurationBuilder = new ClientConfigurationBuilder();
            ClientConfiguration clientConfiguration = configurationBuilder //
                    .connectedTo(serviceHost) //
                    .withProxy(proxyHost) //
                    .build();
            ClientUnderTest clientUnderTest = clientUnderTestFactory.create(clientConfiguration);

            boolean result = clientUnderTest.ping();

            assertThat(result).isTrue();
            verify(headRequestedFor(urlEqualTo("/")));
            hoverfly.verify(service(serviceHost).head("/"), atLeast(1));
        });
    }

    @ParameterizedTest(name = "{0}") // DATAES-801, DATAES-588
    @MethodSource("clientUnderTestFactorySource")
    @DisplayName("should configure client and set all required headers")
    void shouldConfigureClientAndSetAllRequiredHeaders(ClientUnderTestFactory clientUnderTestFactory) {

        wireMockServer(server -> {
            HttpHeaders defaultHeaders = new HttpHeaders();
            defaultHeaders.addAll("def1", Arrays.asList("def1-1", "def1-2"));
            defaultHeaders.add("def2", "def2-1");

            AtomicInteger supplierCount = new AtomicInteger(1);
            AtomicInteger clientConfigurerCount = new AtomicInteger(0);

            ClientConfigurationBuilder configurationBuilder = new ClientConfigurationBuilder();
            configurationBuilder //
                    .connectedTo("localhost:" + server.port()) //
                    .withBasicAuth("user", "password") //
                    .withDefaultHeaders(defaultHeaders) //
                    .withHeaders(() -> {
                        HttpHeaders httpHeaders = new HttpHeaders();
                        httpHeaders.add("supplied", "val0");
                        httpHeaders.add("supplied", "val" + supplierCount.getAndIncrement());
                        return httpHeaders;
                    });

            if (clientUnderTestFactory instanceof ORHLCUnderTestFactory) {
                configurationBuilder.withClientConfigurer(
                        RestClients.RestClientConfigurationCallback.from(httpClientBuilder -> {
                            clientConfigurerCount.incrementAndGet();
                            return httpClientBuilder;
                        }));
            }

            ClientConfiguration clientConfiguration = configurationBuilder.build();

            ClientUnderTest clientUnderTest = clientUnderTestFactory.create(clientConfiguration);

            // do several calls to check that the headerSupplier provided values are set
            int startValue = clientUnderTest.usesInitialRequest() ? 2 : 1;
            for (int i = startValue; i <= startValue + 2; i++) {
                clientUnderTest.ping();

                verify(
                        headRequestedFor(urlEqualTo("/")) //
                                .withHeader("Authorization", new AnythingPattern()) //
                                .withHeader("def1", new EqualToPattern("def1-1")) //
                                .withHeader("def1", new EqualToPattern("def1-2")) //
                                .withHeader("def2", new EqualToPattern("def2-1")) //
                                .withHeader("supplied", new EqualToPattern("val0")) //
                                .withHeader("supplied", new EqualToPattern("val" + (i))) //
                        );
            }

            assertThat(clientConfigurerCount).hasValue(1);
        });
    }

    @ParameterizedTest // #2088
    @MethodSource("clientUnderTestFactorySource")
    @DisplayName("should set compatibility headers")
    void shouldSetCompatibilityHeaders(ClientUnderTestFactory clientUnderTestFactory) {

        wireMockServer(server -> {
            String urlPattern = "^/index/_doc/42(\\?.*)?$";
            stubFor(put(urlMatching(urlPattern)) //
                    .willReturn(jsonResponse(
                                    "{\n" + //
                                            "  \"_id\": \"42\",\n"
                                            + //
                                            "  \"_index\": \"test\",\n"
                                            + //
                                            "  \"_primary_term\": 1,\n"
                                            + //
                                            "  \"_seq_no\": 0,\n"
                                            + //
                                            "  \"_shards\": {\n"
                                            + //
                                            "    \"failed\": 0,\n"
                                            + //
                                            "    \"successful\": 1,\n"
                                            + //
                                            "    \"total\": 2\n"
                                            + //
                                            "  },\n"
                                            + //
                                            "  \"_version\": 1,\n"
                                            + //
                                            "  \"result\": \"created\"\n"
                                            + //
                                            "}\n" //
                                    ,
                                    201) //
                            .withHeader("Content-Type", "application/json")));

            ClientConfigurationBuilder configurationBuilder = new ClientConfigurationBuilder();
            configurationBuilder //
                    .connectedTo("localhost:" + server.port()) //
                    .withHeaders(() -> {
                        HttpHeaders defaultCompatibilityHeaders = new HttpHeaders();
                        defaultCompatibilityHeaders.add("Accept", "application/json");
                        return defaultCompatibilityHeaders;
                    });

            ClientConfiguration clientConfiguration = configurationBuilder.build();
            ClientUnderTest clientUnderTest = clientUnderTestFactory.create(clientConfiguration);

            class Foo {
                public String id;

                Foo(String id) {
                    this.id = id;
                }
            }
            ;

            clientUnderTest.index(new Foo("42"));

            verify(
                    putRequestedFor(urlMatching(urlPattern)) //
                            .withHeader("Accept", new EqualToPattern("application/json")) //
                            .withHeader("Content-Type", new EqualToPattern("application/json")) //
                    );
        });
    }

    private StubMapping stubForOpenSearchVersionCheck() {
        return stubFor(get(urlEqualTo("/")) //
                .willReturn(okJson("{\n" + //
                                "    \"cluster_name\": \"docker-cluster\",\n"
                                + //
                                "    \"cluster_uuid\": \"nKasrfHjRo6ge0eBmMUuAQ\",\n"
                                + //
                                "    \"name\": \"c1a6e517d001\",\n"
                                + //
                                "    \"tagline\": \"You Know, for Search\",\n"
                                + //
                                "    \"version\": {\n"
                                + //
                                "        \"build_date\": \"2021-08-26T09:01:05.390870785Z\",\n"
                                + //
                                "        \"build_flavor\": \"default\",\n"
                                + //
                                "        \"build_hash\": \"66b55ebfa59c92c15db3f69a335d500018b3331e\",\n"
                                + //
                                "        \"build_snapshot\": false,\n"
                                + //
                                "        \"build_type\": \"docker\",\n"
                                + //
                                "        \"lucene_version\": \"8.9.0\",\n"
                                + //
                                "        \"minimum_index_compatibility_version\": \"6.0.0-beta1\",\n"
                                + //
                                "        \"minimum_wire_compatibility_version\": \"6.8.0\",\n"
                                + //
                                "        \"number\": \"7.14.1\"\n"
                                + //
                                "    }\n"
                                + //
                                "}") //
                        .withHeader("Content-Type", "application/json; charset=UTF-8")));
    }

    private StubMapping stubForHead() {
        return stubFor(head(urlEqualTo("/")) //
                .willReturn(ok()));
    }

    /**
     * Consumer extension that catches checked exceptions and wraps them in a RuntimeException.
     */
    @FunctionalInterface
    interface WiremockConsumer extends Consumer<WireMockServer> {
        @Override
        default void accept(WireMockServer wiremockConsumer) {
            try {
                acceptThrows(wiremockConsumer);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        void acceptThrows(WireMockServer wiremockConsumer) throws Exception;
    }

    /**
     * starts a Wiremock server and calls consumer with the server as argument. Stops the server after consumer execution.
     * Before the consumer ids called the {@link #stubForHead()} and {@link #stubForOpenSearchVersionCheck()} are
     * registered.
     *
     * @param consumer the consumer
     */
    private void wireMockServer(WiremockConsumer consumer) {
        WireMockServer wireMockServer = new WireMockServer(
                options() //
                        .dynamicPort() //
                        .usingFilesUnderDirectory(
                                "src/test/resources/wiremock-mappings")); // needed, otherwise Wiremock goes to
        // test/resources/mappings
        try {
            wireMockServer.start();
            WireMock.configureFor(wireMockServer.port());
            stubForHead();
            stubForOpenSearchVersionCheck();

            consumer.accept(wireMockServer);
        } finally {
            wireMockServer.shutdown();
        }
    }

    /**
     * The client to be tested. Abstraction to be able to test reactive and non-reactive clients.
     */
    interface ClientUnderTest {
        /**
         * Pings the configured server. Must use a HEAD request to "/".
         *
         * @return true if successful
         */
        boolean ping() throws Exception;

        /**
         * @return true if the client send an initial request for discovery, version check or similar.
         */
        boolean usesInitialRequest();

        /**
         * save an entity using an index request
         */
        <T> void index(T entity) throws IOException;
    }

    /**
     * base class to create {@link ClientUnderTest} implementations.
     */
    abstract static class ClientUnderTestFactory {
        abstract ClientUnderTest create(ClientConfiguration clientConfiguration);

        @Override
        public String toString() {
            return getDisplayName();
        }

        protected abstract String getDisplayName();
    }

    /**
     * {@link ClientUnderTestFactory} implementation for the old {@link RestHighLevelClient}.
     */
    static class ORHLCUnderTestFactory extends ClientUnderTestFactory {

        @Override
        protected String getDisplayName() {
            return "RestHighLevelClient";
        }

        @Override
        ClientUnderTest create(ClientConfiguration clientConfiguration) {
            RestHighLevelClient client = RestClients.create(clientConfiguration).rest();
            return new ClientUnderTest() {
                @Override
                public boolean ping() throws Exception {
                    return client.ping(RequestOptions.DEFAULT);
                }

                @Override
                public boolean usesInitialRequest() {
                    return false;
                }

                @Override
                public <T> void index(T entity) throws IOException {
                    IndexRequest indexRequest = new IndexRequest("index");
                    indexRequest.id("42");
                    indexRequest.source(entity, XContentType.JSON);
                    client.index(indexRequest, RequestOptions.DEFAULT);
                }
            };
        }
    }

    /**
     * Provides the factories to use in the parameterized tests
     *
     * @return stream of factories
     */
    static Stream<ClientUnderTestFactory> clientUnderTestFactorySource() {
        return Stream.of( //
                new ORHLCUnderTestFactory());
    }
}
