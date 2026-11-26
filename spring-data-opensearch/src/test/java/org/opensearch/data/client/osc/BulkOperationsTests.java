/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.client.transport.OpenSearchTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { BulkOperationsTests.Config.class })
@DisplayName("Using OpenSearch Client Bulk Operations")
class BulkOperationsTests {
    @Autowired private ElasticsearchOperations operations;
    @Autowired private ReactiveElasticsearchOperations reactiveOperations;
    @Autowired private IndexNameProvider indexNameProvider;
    private IndexOperations indexOperations;


    @BeforeEach
    public void before() {
        indexNameProvider.increment();
    }

    @Configuration
    static class Config extends ReactiveOpenSearchConfiguration {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("integration-os");
        }

        @Bean(name = { "elasticsearchOperations", "elasticsearchTemplate", "opensearchOperations", "opensearchTemplate" })
        public ElasticsearchOperations opensearchOperations(OpenSearchTransport transport) {
            final OpenSearchTemplate template = new OpenSearchTemplate(OpenSearchClients.createImperative(transport));
            template.setRefreshPolicy(refreshPolicy());
            template.setBulkOptions(bulkOptions());

            return template;
        }

        @Override
        public ClientConfiguration clientConfiguration() {
            return ClientConfiguration.builder()
                    .connectedTo("localhost:12345")
                    .withConnectTimeout(Duration.ofSeconds(20)) //
                    .withSocketTimeout(Duration.ofSeconds(20)) //
                    .build();
        }
    }

    @Test
    void shouldStopImperativeRequestsAtTransportFailure() throws IOException {
        final List<IndexQuery> indexQueries = new ArrayList<>();
        indexQueries.add(IndexQuery.builder().withId("1").withObject(Map.of()).build());

        assertThatThrownBy(() -> operations.bulkIndex(indexQueries,
                BulkOptions.builder().withMaxOperations(1).build(),
                IndexCoordinates.of(indexNameProvider.indexName())))
            .hasRootCauseInstanceOf(ConnectException.class);
    }

    @Test
    void shouldStopReactiveRequestsAtTransportFailure() throws IOException {
        final List<UpdateQuery> updateQueries = new ArrayList<>();
        updateQueries.add(UpdateQuery.builder("1").withDocument(Document.create()).build());

        StepVerifier.create(reactiveOperations.bulkUpdate(updateQueries,
                BulkOptions.builder().withMaxOperations(1).build(), IndexCoordinates.of(indexNameProvider.indexName())))
                .expectErrorSatisfies(error -> assertThat(error).hasRootCauseInstanceOf(ConnectException.class)).verify();
    }
}
