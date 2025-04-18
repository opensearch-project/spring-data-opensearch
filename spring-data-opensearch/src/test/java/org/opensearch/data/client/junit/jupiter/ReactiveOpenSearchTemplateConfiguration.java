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
import org.opensearch.data.client.osc.ReactiveOpenSearchConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.junit.jupiter.ClusterConnectionInfo;

/**
 * Configuration for Spring Data Elasticsearch tests using an {@link ReactiveElasticsearchTemplate}.
 */
@Configuration
public class ReactiveOpenSearchTemplateConfiguration extends ReactiveOpenSearchConfiguration {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired private ClusterConnectionInfo clusterConnectionInfo;

    @Override
    public ClientConfiguration clientConfiguration() {
        String elasticsearchHostPort = clusterConnectionInfo.getHost() + ':' + clusterConnectionInfo.getHttpPort();

        ClientConfiguration.TerminalClientConfigurationBuilder configurationBuilder = ClientConfiguration.builder() //
                .connectedTo(elasticsearchHostPort);

        String proxy = System.getenv("DATAES_ELASTICSEARCH_PROXY");

        if (proxy != null) {
            configurationBuilder = configurationBuilder.withProxy(proxy);
        }

        configurationBuilder = ((ClientConfiguration.MaybeSecureClientConfigurationBuilder) configurationBuilder)
                .usingSsl(clusterConnectionInfo.isUseSsl());

        String user = System.getenv("DATAES_ELASTICSEARCH_USER");
        String password = System.getenv("DATAES_ELASTICSEARCH_PASSWORD");

        if (hasText(user) && hasText(password)) {
            configurationBuilder.withBasicAuth(user, password);
        }

        // noinspection UnnecessaryLocalVariable
        ClientConfiguration clientConfiguration = configurationBuilder //
                .withConnectTimeout(Duration.ofSeconds(20)) //
                .withSocketTimeout(Duration.ofSeconds(20)) //
                .build();

        return clientConfiguration;
    }

    @Override
    protected RefreshPolicy refreshPolicy() {
        return RefreshPolicy.IMMEDIATE;
    }
}
