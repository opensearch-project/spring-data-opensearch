/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import java.util.List;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientConfigurations.RestClientBuilderConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientConfigurations.RestClientConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientConfigurations.RestClientSnifferConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenSearch REST clients.
 *
 * Adaptation of the {@link org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration} to
 * the needs of OpenSearch.
 */
@AutoConfiguration
@ConditionalOnClass(RestClientBuilder.class)
@EnableConfigurationProperties(OpenSearchProperties.class)
@Import({RestClientBuilderConfiguration.class, RestClientConfiguration.class, RestClientSnifferConfiguration.class})
public class OpenSearchRestClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OpenSearchConnectionDetails.class)
    OpenSearchConnectionDetails openSearchConnectionDetails(OpenSearchProperties properties) {
        return new PropertiesOpenSearchConnectionDetails(properties);
    }

    static class PropertiesOpenSearchConnectionDetails implements OpenSearchConnectionDetails {

        private final OpenSearchProperties properties;

        PropertiesOpenSearchConnectionDetails(OpenSearchProperties properties) {
            this.properties = properties;
        }

        @Override
        public List<String> getUris() {
            return this.properties.getUris();
        }

        @Override
        public String getUsername() {
            return this.properties.getUsername();
        }

        @Override
        public String getPassword() {
            return this.properties.getPassword();
        }

        @Override
        public String getPathPrefix() {
            return this.properties.getPathPrefix();
        }
    }

}
