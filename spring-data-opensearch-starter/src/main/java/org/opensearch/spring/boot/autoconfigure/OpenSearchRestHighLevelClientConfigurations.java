/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

/**
 * OpenSearch REST High LeveL client configurations.
 */
class OpenSearchRestHighLevelClientConfigurations {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(RestClientBuilder.class)
    @ConditionalOnMissingBean(name = {"elasticsearchOperations", "elasticsearchTemplate", "opensearchTemplate"})
    static class RestHighLevelClientConfiguration extends AbstractOpenSearchConfiguration {
        private final RestClientBuilder restClientBuilder;

        RestHighLevelClientConfiguration(RestClientBuilder restClientBuilder) {
            this.restClientBuilder = restClientBuilder;
        }

        @Override
        public RestHighLevelClient opensearchClient() {
            return new RestHighLevelClient(restClientBuilder);
        }

        @Bean
        @ConditionalOnMissingBean
        @Override
        public ElasticsearchCustomConversions elasticsearchCustomConversions() {
            return super.elasticsearchCustomConversions();
        }
    }
}
