/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.util.Timeout;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.opensearch.client.Node;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.sniff.Sniffer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link OpenSearchRestClientAutoConfiguration}.
 *
 * Adaptation of the {@link org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfigurationTests} to
 * the needs of OpenSearch.
 */
class OpenSearchRestClientAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenSearchRestClientAutoConfiguration.class));

    @Test
    void configureShouldCreateRestClientBuilderAndRestClient() {
        this.contextRunner.run((context) ->
                assertThat(context).hasSingleBean(RestClient.class).hasSingleBean(RestClientBuilder.class));
    }

    @Test
    void configureWhenElasticsearchRestClientAutoConfigurationIsPresent() {
        this.contextRunner
                .withUserConfiguration(ElasticsearchRestClientConfiguration.class)
                .run((context) ->
                        assertThat(context).hasSingleBean(RestClient.class).hasSingleBean(RestClientBuilder.class));
    }

    @Test
    void configureWhenCustomRestClientShouldBackOff() {
        this.contextRunner
                .withUserConfiguration(CustomRestClientConfiguration.class)
                .run((context) -> assertThat(context)
                        .hasSingleBean(RestClientBuilder.class)
                        .hasSingleBean(RestClient.class)
                        .hasBean("customRestClient"));
    }

    @Test
    void configureWhenTwoCustomRestClients() {
        this.contextRunner
                .withUserConfiguration(TwoCustomRestClientConfiguration.class)
                .run((context) -> assertThat(context)
                        .hasSingleBean(RestClientBuilder.class)
                        .hasBean("customRestClient")
                        .hasBean("customRestClient1"));
    }

    @Test
    void configureWhenBuilderCustomizerShouldApply() {
        this.contextRunner
                .withUserConfiguration(BuilderCustomizerConfiguration.class)
                .run((context) -> {
                    assertThat(context).hasSingleBean(RestClient.class);
                    RestClient restClient = context.getBean(RestClient.class);
                    assertThat(restClient).hasFieldOrPropertyWithValue("pathPrefix", "/test");
                    assertThat(restClient)
                            .extracting("client.manager.pool.maxTotal")
                            .isEqualTo(100);
                    assertThat(restClient)
                            .extracting("client.defaultConfig.cookieSpec")
                            .isEqualTo("rfc6265-lax");
                });
    }

    @Test
    void configureWithNoTimeoutsApplyDefaults() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasSingleBean(RestClient.class);
            RestClient restClient = context.getBean(RestClient.class);
            assertTimeouts(
                    restClient,
                    Duration.ofMillis(RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS),
                    Duration.ofMillis(RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS));
        });
    }

    @Test
    void configureWithCustomTimeouts() {
        this.contextRunner
                .withPropertyValues("opensearch.connection-timeout=15s", "opensearch.socket-timeout=1m")
                .run((context) -> {
                    assertThat(context).hasSingleBean(RestClient.class);
                    RestClient restClient = context.getBean(RestClient.class);
                    assertTimeouts(restClient, Duration.ofSeconds(15), Duration.ofSeconds(15));
                });
    }

    @Test
    void configureUriWithNoScheme() {
        this.contextRunner.withPropertyValues("opensearch.uris=localhost:9876").run((context) -> {
            RestClient client = context.getBean(RestClient.class);
            assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
                    .containsExactly("http://localhost:9876");
        });
    }

    @Test
    void configureUriWithUsernameOnly() {
        this.contextRunner
                .withPropertyValues("opensearch.uris=http://user@localhost:9200")
                .run((context) -> {
                    RestClient client = context.getBean(RestClient.class);
                    assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
                            .containsExactly("http://localhost:9200");
                    assertThat(client)
                            .extracting(
                                    "client.credentialsProvider",
                                    InstanceOfAssertFactories.type(CredentialsProvider.class))
                            .satisfies((credentialsProvider) -> {
                                Credentials credentials =
                                        credentialsProvider.getCredentials(new AuthScope("localhost", 9200), HttpClientContext.create());
                                assertThat(credentials.getUserPrincipal().getName())
                                        .isEqualTo("user");
                                assertThat(credentials.getPassword()).isNull();
                            });
                });
    }

    @Test
    void configureUriWithUsernameAndEmptyPassword() {
        this.contextRunner
                .withPropertyValues("opensearch.uris=http://user:@localhost:9200")
                .run((context) -> {
                    RestClient client = context.getBean(RestClient.class);
                    assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
                            .containsExactly("http://localhost:9200");
                    assertThat(client)
                            .extracting(
                                    "client.credentialsProvider",
                                    InstanceOfAssertFactories.type(CredentialsProvider.class))
                            .satisfies((credentialsProvider) -> {
                                Credentials credentials =
                                        credentialsProvider.getCredentials(new AuthScope("localhost", 9200),
                                                HttpClientContext.create());
                                assertThat(credentials.getUserPrincipal().getName())
                                        .isEqualTo("user");
                                assertThat(credentials.getPassword()).isEmpty();
                            });
                });
    }

    @Test
    void configureUriWithUsernameAndPasswordWhenUsernameAndPasswordPropertiesSet() {
        this.contextRunner
                .withPropertyValues(
                        "opensearch.uris=http://user:password@localhost:9200,localhost:9201",
                        "opensearch.username=admin",
                        "opensearch.password=admin")
                .run((context) -> {
                    RestClient client = context.getBean(RestClient.class);
                    assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
                            .containsExactly("http://localhost:9200", "http://localhost:9201");
                    assertThat(client)
                            .extracting(
                                    "client.credentialsProvider",
                                    InstanceOfAssertFactories.type(CredentialsProvider.class))
                            .satisfies((credentialsProvider) -> {
                                Credentials uriCredentials =
                                        credentialsProvider.getCredentials(new AuthScope("localhost", 9200),
                                                HttpClientContext.create());
                                assertThat(uriCredentials.getUserPrincipal().getName())
                                        .isEqualTo("user");
                                assertThat(uriCredentials.getPassword()).isEqualTo("password".toCharArray());
                                Credentials defaultCredentials =
                                        credentialsProvider.getCredentials(new AuthScope("localhost", 9201),
                                                HttpClientContext.create());
                                assertThat(defaultCredentials.getUserPrincipal().getName())
                                        .isEqualTo("admin");
                                assertThat(defaultCredentials.getPassword()).isEqualTo("admin".toCharArray());
                            });
                });
    }

    @Test
    void configureWithCustomPathPrefix() {
        this.contextRunner
                .withPropertyValues("opensearch.path-prefix=/some/prefix")
                .run((context) -> {
                    RestClient client = context.getBean(RestClient.class);
                    assertThat(client).extracting("pathPrefix").isEqualTo("/some/prefix");
                });
    }

    @Test
    void configureWithoutSnifferLibraryShouldNotCreateSniffer() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader("org.opensearch.client.sniff"))
                .run((context) ->
                        assertThat(context).hasSingleBean(RestClient.class).doesNotHaveBean(Sniffer.class));
    }

    @Test
    void configureShouldCreateSnifferUsingRestClient() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasSingleBean(Sniffer.class);
            assertThat(context.getBean(Sniffer.class))
                    .hasFieldOrPropertyWithValue("restClient", context.getBean(RestClient.class));
            // Validate shutdown order as the sniffer must be shutdown before the
            // client
            assertThat(context.getBeanFactory().getDependentBeans("opensearchRestClient"))
                    .contains("opensearchSniffer");
        });
    }

    @Test
    void configureWithCustomSnifferSettings() {
        this.contextRunner
                .withPropertyValues(
                        "opensearch.restclient.sniffer.interval=180s",
                        "opensearch.restclient.sniffer.delay-after-failure=30s")
                .run((context) -> {
                    assertThat(context).hasSingleBean(Sniffer.class);
                    Sniffer sniffer = context.getBean(Sniffer.class);
                    assertThat(sniffer)
                            .hasFieldOrPropertyWithValue(
                                    "sniffIntervalMillis", Duration.ofMinutes(3).toMillis());
                    assertThat(sniffer)
                            .hasFieldOrPropertyWithValue(
                                    "sniffAfterFailureDelayMillis",
                                    Duration.ofSeconds(30).toMillis());
                });
    }

    @Test
    void configureWhenCustomSnifferShouldBackOff() {
        Sniffer customSniffer = mock(Sniffer.class);
        this.contextRunner.withBean(Sniffer.class, () -> customSniffer).run((context) -> {
            assertThat(context).hasSingleBean(Sniffer.class);
            Sniffer sniffer = context.getBean(Sniffer.class);
            assertThat(sniffer).isSameAs(customSniffer);
            then(customSniffer).shouldHaveNoInteractions();
        });
    }

    @Test
    void configureWithSslBundle() {
        this.contextRunner
                .withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
                .withPropertyValues(
                        "opensearch.restclient.ssl.bundle=opensearch-ca",
                        "spring.ssl.bundle.pem.opensearch-ca.truststore.certificate=classpath:opensearch-demo-ca.pem",
                        "spring.ssl.bundle.pem.opensearch-ca.options.ciphers=DESede",
                        "spring.ssl.bundle.pem.opensearch-ca.options.enabled-protocols=TLSv1.3"
                )
                .run((context) -> {
                    assertThat(context).hasSingleBean(RestClient.class);
                    RestClient restClient = context.getBean(RestClient.class);
                    Object client = ReflectionTestUtils.getField(restClient, "client");
                    Object connmgr = ReflectionTestUtils.getField(client, "manager");
                    Object connectionOperator = ReflectionTestUtils.getField(connmgr, "connectionOperator");

                    Lookup<TlsStrategy> registry = (Lookup<TlsStrategy>) ReflectionTestUtils.getField(connectionOperator, "tlsStrategyLookup");
                    TlsStrategy strategy = registry.lookup("https");
                    assertThat(strategy).extracting("sslContext").isNotNull();
                    assertThat(strategy).extracting("supportedCipherSuites")
                            .asInstanceOf(InstanceOfAssertFactories.ARRAY)
                            .containsExactly("DESede");
                    assertThat(strategy).extracting("supportedProtocols")
                            .asInstanceOf(InstanceOfAssertFactories.ARRAY)
                            .containsExactly("TLSv1.3");

                });
    }

    @Configuration(proxyBeanMethods = false)
    static class BuilderCustomizerConfiguration {

        @Bean
        RestClientBuilderCustomizer myCustomizer() {
            return new RestClientBuilderCustomizer() {

                @Override
                public void customize(RestClientBuilder builder) {
                    builder.setPathPrefix("/test");
                }

                @Override
                public void customize(HttpAsyncClientBuilder builder) {
                    builder.setConnectionManager(PoolingAsyncClientConnectionManagerBuilder
                        .create().setMaxConnTotal(100).build());
                }

                @Override
                public void customize(RequestConfig.Builder builder) {
                    builder.setCookieSpec("rfc6265-lax");
                }
            };
        }
    }

    private static void assertTimeouts(RestClient restClient, Duration connectTimeout, Duration readTimeout) {
        assertThat(restClient)
                .extracting("client.defaultConfig.connectionRequestTimeout")
                .isEqualTo(Timeout.ofMilliseconds(readTimeout.toMillis()));
        assertThat(restClient)
                .extracting("client.defaultConfig.connectTimeout")
                .isEqualTo(Timeout.ofMilliseconds(connectTimeout.toMillis()));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRestClientConfiguration {

        @Bean
        RestClient customRestClient(RestClientBuilder builder) {
            return builder.build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TwoCustomRestClientConfiguration {

        @Bean
        RestClient customRestClient(RestClientBuilder builder) {
            return builder.build();
        }

        @Bean
        RestClient customRestClient1(RestClientBuilder builder) {
            return builder.build();
        }
    }

    @ImportAutoConfiguration({ElasticsearchRestClientAutoConfiguration.class})
    static class ElasticsearchRestClientConfiguration {}
}
