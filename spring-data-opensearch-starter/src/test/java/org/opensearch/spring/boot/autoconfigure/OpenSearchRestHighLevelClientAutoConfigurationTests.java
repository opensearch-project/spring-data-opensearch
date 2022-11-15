/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration;
import org.opensearch.data.client.orhlc.ClientConfiguration;
import org.opensearch.data.client.orhlc.RestClients;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link OpenSearchRestHighLevelClientAutoConfiguration}.
 */
class OpenSearchRestHighLevelClientAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader("org.opensearch.client.sniff"))
            .withConfiguration(AutoConfigurations.of(OpenSearchRestHighLevelClientAutoConfiguration.class));

    @Test
    void configureShouldNotCreateHighLevelRestClient() {
        this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(RestHighLevelClient.class));
    }

    @Test
    void configureShouldCreateHighLevelRestClient() {
        this.contextRunner
                .withConfiguration(AutoConfigurations.of(OpenSearchRestClientAutoConfiguration.class))
                .run((context) -> assertThat(context).hasSingleBean(RestHighLevelClient.class));
    }

    @Test
    void configureWhenCustomRestCreateHighLevelRestClientShouldBackOff() {
        this.contextRunner
                .withUserConfiguration(CustomOpenSearchConfiguration.class)
                .withConfiguration(AutoConfigurations.of(OpenSearchRestClientAutoConfiguration.class))
                .run((context) -> assertThat(context).hasSingleBean(RestHighLevelClient.class));
    }

    @Configuration
    static class CustomOpenSearchConfiguration extends AbstractOpenSearchConfiguration {
        @Override
        public RestHighLevelClient opensearchClient() {

            final ClientConfiguration clientConfiguration =
                    ClientConfiguration.builder().connectedTo("localhost:9200").build();

            return RestClients.create(clientConfiguration).rest();
        }
    }
}
