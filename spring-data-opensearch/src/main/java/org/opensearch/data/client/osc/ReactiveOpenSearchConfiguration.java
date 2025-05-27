/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.osc;

import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;
import org.opensearch.client.transport.rest_client.RestClientOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.util.Assert;

/**
 * Base class for a @{@link org.springframework.context.annotation.Configuration} class to set up the OpenSearch
 * connection using the {@link ReactiveOpenSearchClient}. This class exposes different parts of the setup as Spring
 * beans. Deriving * classes must provide the {@link ClientConfiguration} to use.
 */
public abstract class ReactiveOpenSearchConfiguration extends ElasticsearchConfigurationSupport {

    /**
     * Must be implemented by deriving classes to provide the {@link ClientConfiguration}.
     *
     * @return configuration, must not be {@literal null}
     */
    @Bean(name = "elasticsearchClientConfiguration")
    public abstract ClientConfiguration clientConfiguration();

    /**
     * Provides the underlying low level RestClient.
     *
     * @param clientConfiguration configuration for the client, must not be {@literal null}
     * @return RestClient
     */
    @Bean
    public RestClient elasticsearchRestClient(ClientConfiguration clientConfiguration) {

        Assert.notNull(clientConfiguration, "clientConfiguration must not be null");

        return OpenSearchClients.getRestClient(clientConfiguration);
    }

    /**
     * Provides the Elasticsearch transport to be used. The default implementation uses the {@link RestClient} bean and
     * the {@link JsonpMapper} bean provided in this class.
     *
     * @return the {@link OpenSearchTransport}
     * @since 5.2
     */
    @Bean
    public OpenSearchTransport opensearchTransport(RestClient restClient, JsonpMapper jsonpMapper) {

        Assert.notNull(restClient, "restClient must not be null");
        Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

        return OpenSearchClients.getOpenSearchTransport(restClient, OpenSearchClients.REACTIVE_CLIENT,
                transportOptions(), jsonpMapper);
    }

    /**
     * Provides the {@link ReactiveOpenSearchClient} instance used.
     *
     * @param transport the OpenSearchTransport to use
     * @return ReactiveOpenSearchClient instance.
     */
    @Bean
    public ReactiveOpenSearchClient reactiveOpenSearchClient(OpenSearchTransport transport) {

        Assert.notNull(transport, "transport must not be null");

        return OpenSearchClients.createReactive(transport);
    }

    /**
     * Creates {@link ReactiveElasticsearchOperations}.
     *
     * @return never {@literal null}.
     */
    @Bean(name = { "reactiveElasticsearchOperations", "reactiveElasticsearchTemplate", "reactiveOpensearchOperations", "reactiveOpensearchTemplate" })
    public ReactiveElasticsearchOperations reactiveElasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
            ReactiveOpenSearchClient reactiveElasticsearchClient) {

        ReactiveOpenSearchTemplate template = new ReactiveOpenSearchTemplate(reactiveElasticsearchClient,
                elasticsearchConverter);
        template.setRefreshPolicy(refreshPolicy());

        return template;
    }

    /**
     * Provides the JsonpMapper that is used in the {@link #opensearchTransport(RestClient, JsonpMapper)} method and
     * exposes it as a bean.
     *
     * @return the {@link JsonpMapper} to use
     * @since 5.2
     */
    @Bean
    public JsonpMapper jsonpMapper() {
        return new JacksonJsonpMapper();
    }

    /**
     * @return the options that should be added to every request. Must not be {@literal null}
     */
    public TransportOptions transportOptions() {
        return new RestClientOptions(RequestOptions.DEFAULT).toBuilder().build();
    }
}
