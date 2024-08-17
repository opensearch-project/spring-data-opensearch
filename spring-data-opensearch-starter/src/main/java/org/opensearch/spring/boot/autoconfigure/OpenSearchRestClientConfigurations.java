/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * OpenSearch REST client configurations.
 *
 * Adaptation of the {@link org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientConfigurations} to
 * the needs of OpenSearch.
 */
class OpenSearchRestClientConfigurations {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(RestClientBuilder.class)
    static class RestClientBuilderConfiguration {

        private final OpenSearchConnectionDetails connectionDetails;

        RestClientBuilderConfiguration(OpenSearchConnectionDetails connectionDetails) {
            this.connectionDetails = connectionDetails;
        }

        @Bean
        RestClientBuilderCustomizer defaultRestClientBuilderCustomizer(OpenSearchProperties properties) {
            return new DefaultRestClientBuilderCustomizer(properties, this.connectionDetails);
        }

        @Bean
        RestClientBuilder opensearchRestClientBuilder(ObjectProvider<RestClientBuilderCustomizer> builderCustomizers) {
            HttpHost[] hosts =
                    this.connectionDetails.getUris().stream().map(this::createHttpHost).toArray(HttpHost[]::new);
            RestClientBuilder builder = RestClient.builder(hosts);
            builder.setHttpClientConfigCallback((httpClientBuilder) -> {
                builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(httpClientBuilder));
                return httpClientBuilder;
            });
            builder.setRequestConfigCallback((requestConfigBuilder) -> {
                builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(requestConfigBuilder));
                return requestConfigBuilder;
            });
            if (this.connectionDetails.getPathPrefix() != null) {
                builder.setPathPrefix(this.connectionDetails.getPathPrefix());
            }
            builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
            return builder;
        }

        private HttpHost createHttpHost(String uri) {
            try {
                return createHttpHost(URI.create(uri));
            } catch (IllegalArgumentException ex) {
                return HttpHost.create(uri);
            }
        }

        private HttpHost createHttpHost(URI uri) {
            if (!StringUtils.hasLength(uri.getUserInfo())) {
                return HttpHost.create(uri.toString());
            }
            try {
                return HttpHost.create(new URI(
                                uri.getScheme(),
                                null,
                                uri.getHost(),
                                uri.getPort(),
                                uri.getPath(),
                                uri.getQuery(),
                                uri.getFragment())
                        .toString());
            } catch (URISyntaxException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(RestClient.class)
    static class RestClientConfiguration {

        @Bean
        RestClient opensearchRestClient(RestClientBuilder restClientBuilder) {
            return restClientBuilder.build();
        }
    }

    static class DefaultRestClientBuilderCustomizer implements RestClientBuilderCustomizer {

        private static final PropertyMapper map = PropertyMapper.get();

        private final OpenSearchProperties properties;

        private final OpenSearchConnectionDetails connectionDetails;

        DefaultRestClientBuilderCustomizer(OpenSearchProperties properties, OpenSearchConnectionDetails connectionDetails) {
            this.properties = properties;
            this.connectionDetails = connectionDetails;
        }

        @Override
        public void customize(RestClientBuilder builder) {}

        @Override
        public void customize(HttpAsyncClientBuilder builder) {
            builder.setDefaultCredentialsProvider(new ConnectionsDetailsCredentialsProvider(this.connectionDetails));
            map.from(this.properties::isSocketKeepAlive)
                    .to((keepAlive) -> builder.setDefaultIOReactorConfig(
                            IOReactorConfig.custom().setSoKeepAlive(keepAlive).build()));
        }

        @Override
        public void customize(RequestConfig.Builder builder) {
            map.from(this.properties::getConnectionTimeout)
                    .whenNonNull()
                    .asInt(Duration::toMillis)
                    .to(builder::setConnectTimeout);
            map.from(this.properties::getSocketTimeout)
                    .whenNonNull()
                    .asInt(Duration::toMillis)
                    .to(builder::setSocketTimeout);
        }
    }

    private static class ConnectionsDetailsCredentialsProvider extends BasicCredentialsProvider {

        ConnectionsDetailsCredentialsProvider(OpenSearchConnectionDetails connectionDetails) {
            if (StringUtils.hasText(connectionDetails.getUsername())) {
                Credentials credentials =
                        new UsernamePasswordCredentials(connectionDetails.getUsername(), connectionDetails.getPassword());
                setCredentials(AuthScope.ANY, credentials);
            }
            connectionDetails.getUris().stream()
                    .map(this::toUri)
                    .filter(this::hasUserInfo)
                    .forEach(this::addUserInfoCredentials);
        }

        private URI toUri(String uri) {
            try {
                return URI.create(uri);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        private boolean hasUserInfo(URI uri) {
            return uri != null && StringUtils.hasLength(uri.getUserInfo());
        }

        private void addUserInfoCredentials(URI uri) {
            AuthScope authScope = new AuthScope(uri.getHost(), uri.getPort());
            Credentials credentials = createUserInfoCredentials(uri.getUserInfo());
            setCredentials(authScope, credentials);
        }

        private Credentials createUserInfoCredentials(String userInfo) {
            int delimiter = userInfo.indexOf(":");
            if (delimiter == -1) {
                return new UsernamePasswordCredentials(userInfo, null);
            }
            String username = userInfo.substring(0, delimiter);
            String password = userInfo.substring(delimiter + 1);
            return new UsernamePasswordCredentials(username, password);
        }
    }
}
