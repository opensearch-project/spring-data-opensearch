/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.junit.jupiter;

import static org.springframework.util.StringUtils.*;

import java.time.Duration;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration;
import org.opensearch.data.client.orhlc.ClientConfiguration;
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;
import org.opensearch.data.client.orhlc.RestClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.junit.jupiter.ClusterConnectionInfo;

/**
 * Configuration for Spring Data OpenSearch using {@link OpenSearchRestTemplate}.
 */
@Configuration
public class OpenSearchRestTemplateConfiguration extends AbstractOpenSearchConfiguration {

    @Autowired
    private ClusterConnectionInfo clusterConnectionInfo;

    @Override
    @Bean
    public RestHighLevelClient opensearchClient() {

        String elasticsearchHostPort = clusterConnectionInfo.getHost() + ':' + clusterConnectionInfo.getHttpPort();

        ClientConfiguration.TerminalClientConfigurationBuilder configurationBuilder =
                ClientConfiguration.builder().connectedTo(elasticsearchHostPort);

        String proxy = System.getenv("DATAOS_OPENSEARCH_PROXY");

        if (proxy != null) {
            configurationBuilder = configurationBuilder.withProxy(proxy);
        }

        if (clusterConnectionInfo.isUseSsl()) {
            configurationBuilder =
                    ((ClientConfiguration.MaybeSecureClientConfigurationBuilder) configurationBuilder).usingSsl();
        }

        String user = System.getenv("DATAOS_OPENSEARCH_USER");
        String password = System.getenv("DATAOS_OPENSEARCH_PASSWORD");

        if (hasText(user) && hasText(password)) {
            configurationBuilder.withBasicAuth(user, password);
        }

        return RestClients.create(
                        configurationBuilder //
                                .withConnectTimeout(Duration.ofSeconds(20)) //
                                .withSocketTimeout(Duration.ofSeconds(20)) //
                                .build()) //
                .rest();
    }

    @Override
    public ElasticsearchOperations elasticsearchOperations(
            ElasticsearchConverter elasticsearchConverter, RestHighLevelClient elasticsearchClient) {

        OpenSearchRestTemplate template = new OpenSearchRestTemplate(elasticsearchClient, elasticsearchConverter) {
            @Override
            public <T> T execute(ClientCallback<T> callback) {
                try {
                    return super.execute(callback);
                } catch (DataAccessResourceFailureException e) {
                    try {
                        Thread.sleep(1_000);
                    } catch (InterruptedException ignored) {
                    }
                    return super.execute(callback);
                }
            }
        };
        template.setRefreshPolicy(refreshPolicy());

        return template;
    }

    @Override
    protected RefreshPolicy refreshPolicy() {
        return RefreshPolicy.IMMEDIATE;
    }
}
