/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;


import org.opensearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

/**
 * @see ElasticsearchConfigurationSupport
 * @since 0.1
 */
public abstract class AbstractOpenSearchConfiguration extends ElasticsearchConfigurationSupport {

    /**
     * Return the {@link RestHighLevelClient} instance used to connect to the cluster. <br />
     *
     * @return never {@literal null}.
     */
    @Bean
    public abstract RestHighLevelClient opensearchClient();

    /**
     * Creates {@link ElasticsearchOperations}.
     *
     * @return never {@literal null}.
     */
    @Bean(name = {"elasticsearchOperations", "elasticsearchTemplate"})
    public ElasticsearchOperations elasticsearchOperations(
            ElasticsearchConverter elasticsearchConverter, RestHighLevelClient elasticsearchClient) {

        OpenSearchRestTemplate template = new OpenSearchRestTemplate(elasticsearchClient, elasticsearchConverter);
        template.setRefreshPolicy(refreshPolicy());

        return template;
    }
}
