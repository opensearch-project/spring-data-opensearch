/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import org.opensearch.client.RestClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

@AutoConfiguration(after = OpenSearchClientAutoConfiguration.class)
@ConditionalOnBean(RestClient.class)
@ConditionalOnClass({ OpenSearchTransport.class, Mono.class })
@EnableConfigurationProperties(OpenSearchProperties.class)
@Import({ OpenSearchClientConfigurations.JsonpMapperConfiguration.class,
    OpenSearchClientConfigurations.OpenSearchTransportConfiguration.class })
public class ReactiveOpenSearchClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OpenSearchTransport.class)
    ReactiveOpenSearchClient reactiveOpensearchClient(OpenSearchTransport transport) {
        return new ReactiveOpenSearchClient(transport);
    }

}
