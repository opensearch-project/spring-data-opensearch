/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.json.jsonb.JsonbJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientOptions;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class OpenSearchClientConfigurations {
    @Import({ JacksonJsonpMapperConfiguration.class, JsonbJsonpMapperConfiguration.class })
    static class JsonpMapperConfiguration {
    }

    @ConditionalOnMissingBean(JsonpMapper.class)
    @ConditionalOnClass(ObjectMapper.class)
    @Configuration(proxyBeanMethods = false)
    static class JacksonJsonpMapperConfiguration {
        @Bean
        JacksonJsonpMapper jacksonJsonpMapper() {
            return new JacksonJsonpMapper();
        }
    }

    @ConditionalOnMissingBean(JsonpMapper.class)
    @ConditionalOnBean(Jsonb.class)
    @Configuration(proxyBeanMethods = false)
    static class JsonbJsonpMapperConfiguration {
        @Bean
        JsonbJsonpMapper jsonbJsonpMapper(Jsonb jsonb) {
            return new JsonbJsonpMapper(JsonProvider.provider(), jsonb);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(OpenSearchTransport.class)
    static class OpenSearchTransportConfiguration {
        @Bean
        RestClientTransport restClientTransport(RestClient restClient, JsonpMapper jsonMapper,
                ObjectProvider<RestClientOptions> restClientOptions) {
            return new RestClientTransport(restClient, jsonMapper, restClientOptions.getIfAvailable());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(OpenSearchTransport.class)
    static class OpenSearchClientConfiguration {
        @Bean
        @ConditionalOnMissingBean
        OpenSearchClient opensearchClient(OpenSearchTransport transport) {
            return new OpenSearchClient(transport);
        }
    }
}
