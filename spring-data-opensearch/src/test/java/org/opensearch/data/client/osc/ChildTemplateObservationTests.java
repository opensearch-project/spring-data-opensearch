/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.data.core.OpenSearchMappingParametersCustomizer;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ChildTemplateObservationTests {
    private static final IndexCoordinates INDEX = IndexCoordinates.of("observed");
    private final OpenSearchClient client = mock(OpenSearchClient.class, RETURNS_DEEP_STUBS);
    private final ReactiveOpenSearchClient reactiveClient = mock(ReactiveOpenSearchClient.class, RETURNS_DEEP_STUBS);
    private final MappingElasticsearchConverter converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
    private final ObservationRecorder recorder = new ObservationRecorder();
    private OpenSearchTemplate template;
    private ReactiveOpenSearchTemplate reactiveTemplate;

    @BeforeEach
    void setUp() {
        when(client._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(client.indices()._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(client.cluster()._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(reactiveClient._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(reactiveClient.indices()._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(reactiveClient.cluster()._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        template = new OpenSearchTemplate(client, converter, new OpenSearchMappingParametersCustomizer());
        reactiveTemplate = new ReactiveOpenSearchTemplate(reactiveClient, converter);
    }

    @Test
    void childTemplatesFollowParentConfigurationEvenAfterCreation() throws Exception {
        when(client.indices().exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        when(reactiveClient.indices().exists(any(ExistsRequest.class))).thenReturn(Mono.just(new BooleanResponse(true)));
        final var index = template.indexOps(INDEX);
        final var reactiveIndex = reactiveTemplate.indexOps(INDEX);
        template.setObservationRegistry(recorder.registry);
        template.setObservationConvention(new OpenSearchTemplateObservationTests.CustomConvention());
        reactiveTemplate.setObservationRegistry(recorder.registry);
        reactiveTemplate.setObservationConvention(new OpenSearchTemplateObservationTests.CustomConvention());
        assertThat(index.exists()).isTrue();
        StepVerifier.create(reactiveIndex.exists()).expectNext(true).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.INDEX_EXISTS, OpenSearchOperationName.INDEX_EXISTS);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("custom.opensearch"));
    }

    @Test
    void copiedParentsConfigureTheirOwnChildren() throws Exception {
        when(client.indices().exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        when(reactiveClient.indices().exists(any(ExistsRequest.class))).thenReturn(Mono.just(new BooleanResponse(true)));
        template.setObservationRegistry(recorder.registry);
        reactiveTemplate.setObservationRegistry(recorder.registry);
        template.withRefreshPolicy(RefreshPolicy.IMMEDIATE).indexOps(OpenSearchTemplateObservationTests.Sample.class).exists();
        StepVerifier.create(reactiveTemplate.withRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .indexOps(OpenSearchTemplateObservationTests.Sample.class).exists()).expectNext(true).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.INDEX_EXISTS, OpenSearchOperationName.INDEX_EXISTS);
    }

    @Test
    void deleteDoesNotRecordItsInternalExistenceCheck() throws Exception {
        when(client.indices().exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        when(client.indices().delete(any(DeleteIndexRequest.class))).thenReturn(DeleteIndexResponse.of(r -> r.acknowledged(true)));
        when(reactiveClient.indices().exists(any(ExistsRequest.class))).thenReturn(Mono.just(new BooleanResponse(true)));
        when(reactiveClient.indices().delete(any(DeleteIndexRequest.class)))
                .thenReturn(Mono.just(DeleteIndexResponse.of(r -> r.acknowledged(true))));
        template.setObservationRegistry(recorder.registry);
        reactiveTemplate.setObservationRegistry(recorder.registry);
        assertThat(template.indexOps(INDEX).delete()).isTrue();
        StepVerifier.create(reactiveTemplate.indexOps(INDEX).delete()).expectNext(true).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.INDEX_DELETE, OpenSearchOperationName.INDEX_DELETE);
    }

    @Test
    void createOverloadsObserveOnlyOneRequest() throws Exception {
        when(client.indices().create(any(CreateIndexRequest.class))).thenReturn(CreateIndexResponse.of(r ->
                r.index("observed").acknowledged(true).shardsAcknowledged(true)));
        when(reactiveClient.indices().create(any(CreateIndexRequest.class))).thenReturn(Mono.just(CreateIndexResponse.of(r ->
                r.index("observed").acknowledged(true).shardsAcknowledged(true))));
        template.setObservationRegistry(recorder.registry);
        reactiveTemplate.setObservationRegistry(recorder.registry);
        assertThat(template.indexOps(OpenSearchTemplateObservationTests.Sample.class).createWithMapping()).isTrue();
        StepVerifier.create(reactiveTemplate.indexOps(OpenSearchTemplateObservationTests.Sample.class).createWithMapping())
                .expectNext(true).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.INDEX_CREATE, OpenSearchOperationName.INDEX_CREATE);
    }

    @Test
    void directlyConstructedChildrenDiscoverBeansAndDefaultToNoop() throws Exception {
        when(client.indices().exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        when(reactiveClient.indices().exists(any(ExistsRequest.class))).thenReturn(Mono.just(new BooleanResponse(true)));
        final var index = new IndicesTemplate(client.indices(), new ClusterTemplate(client.cluster(), converter),
                converter, new OpenSearchMappingParametersCustomizer(), INDEX);
        final var reactiveIndex = new ReactiveIndicesTemplate(reactiveClient.indices(),
                new ReactiveClusterTemplate(reactiveClient.cluster(), converter), converter, INDEX);
        assertThat(index.exists()).isTrue();
        StepVerifier.create(reactiveIndex.exists()).expectNext(true).verifyComplete();
        recorder.assertOperations();
        try (final var context = new StaticApplicationContext()) {
            context.getBeanFactory().registerSingleton("observationRegistry", recorder.registry);
            context.getBeanFactory().registerSingleton("convention", new OpenSearchTemplateObservationTests.CustomConvention());
            index.setApplicationContext(context);
            reactiveIndex.setApplicationContext(context);
            assertThat(index.exists()).isTrue();
            StepVerifier.create(reactiveIndex.exists()).expectNext(true).verifyComplete();
        }
        recorder.assertOperations(OpenSearchOperationName.INDEX_EXISTS, OpenSearchOperationName.INDEX_EXISTS);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("custom.opensearch"));
    }

    @Test
    void localMappingAndSettingsDoNotCreateObservations() {
        template.setObservationRegistry(recorder.registry);
        reactiveTemplate.setObservationRegistry(recorder.registry);
        final var index = template.indexOps(OpenSearchTemplateObservationTests.Sample.class);
        final var reactiveIndex = reactiveTemplate.indexOps(OpenSearchTemplateObservationTests.Sample.class);
        assertThat(index.createMapping()).isNotNull();
        assertThat(index.createSettings()).isNotNull();
        StepVerifier.create(reactiveIndex.createMapping()).expectNextCount(1).verifyComplete();
        StepVerifier.create(reactiveIndex.createSettings()).expectNextCount(1).verifyComplete();
        recorder.assertOperations();
    }

    @Test
    void clusterErrorsAreObservedForParentCreatedAndDirectTemplates() {
        final var failure = new IllegalStateException("failure");
        final var cluster = new ClusterTemplate(client.cluster(), converter) {
            @Override
            public <T> T execute(final ClientCallback<org.opensearch.client.opensearch.cluster.OpenSearchClusterClient, T> callback) {
                throw failure;
            }
        };
        cluster.setObservationRegistry(recorder.registry);
        assertThatThrownBy(cluster::health).isSameAs(failure);
        when(reactiveClient.cluster().health(any(org.opensearch.client.opensearch.cluster.HealthRequest.class)))
                .thenReturn(Mono.error(failure));
        reactiveTemplate.setObservationRegistry(recorder.registry);
        StepVerifier.create(reactiveTemplate.cluster().health()).expectError().verify();
        recorder.assertOperations(OpenSearchOperationName.CLUSTER_HEALTH, OpenSearchOperationName.CLUSTER_HEALTH);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getError()).isNotNull());
    }

    @Test
    void reactiveChildCancellationStopsObservation() {
        when(reactiveClient.indices().exists(any(ExistsRequest.class))).thenReturn(Mono.never());
        reactiveTemplate.setObservationRegistry(recorder.registry);
        StepVerifier.create(reactiveTemplate.indexOps(INDEX).exists()).thenCancel().verify();
        recorder.assertOperations(OpenSearchOperationName.INDEX_EXISTS);
    }

    @Test
    void reactiveMappingFailureIsTranslatedAndObserved() {
        final var failure = new RuntimeException("mapping failure", new IOException("connection closed"));
        when(reactiveClient.indices().putMapping(any(org.opensearch.client.opensearch.indices.PutMappingRequest.class)))
                .thenReturn(Mono.error(failure));
        reactiveTemplate.setObservationRegistry(recorder.registry);
        final var mapping = reactiveTemplate.indexOps(INDEX).putMapping(Mono.just(Document.create()));
        assertThat(recorder.started).isEmpty();

        StepVerifier.create(mapping)
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(DataAccessResourceFailureException.class).hasCause(failure))
                .verify();

        recorder.assertOperations(OpenSearchOperationName.INDEX_PUT_MAPPING);
        assertThat(recorder.stopped.getFirst().getError()).isInstanceOf(DataAccessResourceFailureException.class);
    }
}
