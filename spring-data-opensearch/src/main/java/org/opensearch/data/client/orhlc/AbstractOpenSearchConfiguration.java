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
import org.opensearch.data.core.OpenSearchMappingParametersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.MappingParametersCustomizer;

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
    @Bean(name = {"elasticsearchOperations", "elasticsearchTemplate", "opensearchTemplate"})
    public ElasticsearchOperations elasticsearchOperations(
            ElasticsearchConverter elasticsearchConverter, RestHighLevelClient opensearchClient, MappingParametersCustomizer opensearchMappingParametersCustomizer) {

        OpenSearchRestTemplate template = new OpenSearchRestTemplate(opensearchClient, elasticsearchConverter, opensearchMappingParametersCustomizer);
        template.setRefreshPolicy(refreshPolicy());

        return template;
    }

    @Bean
    public MappingParametersCustomizer opensearchMappingParametersCustomizer() {
        return new OpenSearchMappingParametersCustomizer();
    }
}
