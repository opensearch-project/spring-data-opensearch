/*
 * Copyright 2021-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.data.client.osc;

import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;
import org.opensearch.client.transport.rest_client.RestClientOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.util.Assert;

/**
 * Base class for a @{@link org.springframework.context.annotation.Configuration} class to set up the OpenSearch
 * connection using the OpenSearch Client. This class exposes different parts of the setup as Spring beans. Deriving
 * classes must provide the {@link ClientConfiguration} to use.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public abstract class OpenSearchConfiguration extends ElasticsearchConfigurationSupport {

    /**
     * Must be implemented by deriving classes to provide the {@link ClientConfiguration}.
     *
     * @return configuration, must not be {@literal null}
     */
    @Bean(name = "elasticsearchClientConfiguration")
    public abstract ClientConfiguration clientConfiguration();

    /**
     * Provides the underlying low level OpenSearch RestClient.
     *
     * @param clientConfiguration configuration for the client, must not be {@literal null}
     * @return RestClient
     */
    @Bean
    public RestClient opensearchRestClient(ClientConfiguration clientConfiguration) {

        Assert.notNull(clientConfiguration, "clientConfiguration must not be null");

        return OpenSearchClients.getRestClient(clientConfiguration);
    }

    /**
     * Provides the OpenSearch transport to be used. The default implementation uses the {@link RestClient} bean and
     * the {@link JsonpMapper} bean provided in this class.
     *
     * @return the {@link OpenSearchTransport}
     * @since 5.2
     */
    @Bean
    public OpenSearchTransport opensearchTransport(RestClient restClient, JsonpMapper jsonpMapper) {

        Assert.notNull(restClient, "restClient must not be null");
        Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

        return OpenSearchClients.getOpenSearchTransport(restClient, OpenSearchClients.IMPERATIVE_CLIENT,
                transportOptions(), jsonpMapper);
    }

    /**
     * Provides the {@link OpenSearchClient} to be used.
     *
     * @param transport the {@link OpenSearchTransport} to use
     * @return OpenSearchClient instance
     */
    @Bean
    public OpenSearchClient opensearchClient(OpenSearchTransport transport) {

        Assert.notNull(transport, "transport must not be null");

        return OpenSearchClients.createImperative(transport);
    }

    /**
     * Creates a {@link ElasticsearchOperations} implementation using an
     * {@link org.opensearch.client.opensearch.OpenSearchClient}.
     *
     * @return never {@literal null}.
     */
    @Bean(name = { "elasticsearchOperations", "elasticsearchTemplate", "opensearchOperations", "opensearchTemplate" })
    public ElasticsearchOperations opensearchOperations(ElasticsearchConverter elasticsearchConverter,
            OpenSearchClient elasticsearchClient) {

        OpenSearchTemplate template = new OpenSearchTemplate(elasticsearchClient, elasticsearchConverter);
        template.setRefreshPolicy(refreshPolicy());

        return template;
    }

    /**
     * Provides the JsonpMapper bean that is used in the {@link #opensearchTransport(RestClient, JsonpMapper)} method.
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
        return new RestClientOptions(RequestOptions.DEFAULT);
    }
}
