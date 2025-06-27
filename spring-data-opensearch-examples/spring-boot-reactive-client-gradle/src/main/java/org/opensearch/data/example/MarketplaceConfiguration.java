/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.data.example;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.data.example.repository.ReactiveMarketplaceRepository;
import org.opensearch.spring.boot.autoconfigure.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;

@Configuration
@EnableReactiveElasticsearchRepositories(basePackageClasses = ReactiveMarketplaceRepository.class)
@ComponentScan(basePackageClasses = MarketplaceConfiguration.class)
public class MarketplaceConfiguration {
    /**
     * Allow to connect to the OpenSearch instance which uses self-signed certificates
     */
    @Bean
    RestClientBuilderCustomizer customizer() {
        return new RestClientBuilderCustomizer() {
            @Override
            public void customize(HttpAsyncClientBuilder builder) {
                try {
                    final ClientTlsStrategyBuilder tlsStrategy = ClientTlsStrategyBuilder.create()
                            .setSslContext(new SSLContextBuilder()
                                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                                    .build());

                    final PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder
                            .create()
                            .setTlsStrategy(tlsStrategy.build())
                            .build();

                    builder.setConnectionManager(connectionManager);
                } catch (final KeyManagementException | NoSuchAlgorithmException | KeyStoreException ex) {
                    throw new RuntimeException("Failed to initialize SSL Context instance", ex);
                }
            }

            @Override
            public void customize(RestClientBuilder builder) {
                // No additional customizations needed
            }
        };
    }
}
