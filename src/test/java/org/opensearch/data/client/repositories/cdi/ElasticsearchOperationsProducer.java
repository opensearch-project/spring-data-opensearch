/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.repositories.cdi;

import static org.springframework.util.StringUtils.*;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.opensearch.data.client.orhlc.ClientConfiguration;
import org.opensearch.data.client.orhlc.OpensearchRestTemplate;
import org.opensearch.data.client.orhlc.RestClients;
import org.opensearch.data.client.orhlc.RestClients.OpensearchRestClient;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.junit.jupiter.ClusterConnection;
import org.springframework.data.elasticsearch.junit.jupiter.ClusterConnectionInfo;
import org.springframework.data.elasticsearch.repositories.cdi.OtherQualifier;
import org.springframework.data.elasticsearch.repositories.cdi.PersonDB;

@ApplicationScoped
public class ElasticsearchOperationsProducer {

    @Produces
    public ElasticsearchOperations createElasticsearchTemplate(OpensearchRestClient client) {
        return new OpensearchRestTemplate(client.rest());
    }

    @Produces
    @OtherQualifier
    @PersonDB
    public ElasticsearchOperations createQualifiedOpensearchTemplate(OpensearchRestClient client) {
        return new OpensearchRestTemplate(client.rest());
    }

    @PreDestroy
    public void shutdown() {
        // remove everything to avoid conflicts with other tests in case server not shut down properly
    }

    @Produces
    public OpensearchRestClient opensearchClient() {
        // we rely on the tests being run with the SpringDataElasticsearchExtension class that sets up a containerized
        // ES.
        ClusterConnectionInfo connectionInfo = ClusterConnection.clusterConnectionInfo();

        ClientConfiguration.TerminalClientConfigurationBuilder configurationBuilder = ClientConfiguration.builder() //
                .connectedTo(connectionInfo.getHost() + ':' + connectionInfo.getHttpPort());

        String user = System.getenv("DATAES_ELASTICSEARCH_USER");
        String password = System.getenv("DATAES_ELASTICSEARCH_PASSWORD");

        if (hasText(user) && hasText(password)) {
            configurationBuilder.withBasicAuth(user, password);
        }

        String proxy = System.getenv("DATAES_ELASTICSEARCH_PROXY");

        if (hasText(proxy)) {
            configurationBuilder.withProxy(proxy);
        }

        ClientConfiguration clientConfiguration = configurationBuilder //
                .build();

        return RestClients.create(clientConfiguration);
    }
}
