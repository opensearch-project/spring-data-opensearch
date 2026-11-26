/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.client.json.jackson3.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.bulk.OperationType;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.data.core.OpenSearchMappingParametersCustomizer;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

class BulkOperationsTests {

    private final OpenSearchClient client = mock(OpenSearchClient.class);
    private final ReactiveOpenSearchClient reactiveClient = mock(ReactiveOpenSearchClient.class);
    private final MappingElasticsearchConverter converter =
            new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
    private final IndexCoordinates index = IndexCoordinates.of("index");
    private final List<BulkRequest> requests = new ArrayList<>();
    private OpenSearchTemplate template;
    private ReactiveOpenSearchTemplate reactiveTemplate;

    @BeforeEach
    void setUp() {
        converter.afterPropertiesSet();
        final var transport = mock(OpenSearchTransport.class);
        when(transport.jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(client._transport()).thenReturn(transport);
        when(reactiveClient._transport()).thenReturn(transport);
        template = new OpenSearchTemplate(client, converter, new OpenSearchMappingParametersCustomizer());
        reactiveTemplate = new ReactiveOpenSearchTemplate(reactiveClient, converter);
    }

    @Test
    void shouldCollectItemFailuresFromAllImperativeRequests() throws IOException {
        final var options = BulkOptions.builder().withMaxOperations(1).build();
        when(client.bulk(any(BulkRequest.class)))
                .thenAnswer(invocation -> response(invocation.getArgument(0), Set.of("1", "3")));

        assertThatThrownBy(() -> template.bulkIndex(indexQueries(), options, index))
                .isInstanceOfSatisfying(BulkFailureException.class,
                        exception -> assertThat(exception.getFailedDocuments()).containsOnlyKeys("1", "3"));
        assertThat(requests).hasSize(3);
    }

    @Test
    void shouldStopImperativeRequestsAtTransportFailure() throws IOException {
        when(client.bulk(any(BulkRequest.class))).thenAnswer(invocation -> {
            if (requests.size() == 1) {
                throw new IOException("connection lost");
            }
            return response(invocation.getArgument(0), Set.of());
        });

        assertThatThrownBy(() -> template.bulkIndex(indexQueries(),
                BulkOptions.builder().withMaxOperations(1).build(), index))
                .hasRootCauseMessage("connection lost");
        assertThat(requests).hasSize(1);
        verify(client, times(2)).bulk(any(BulkRequest.class));
    }

    @Test
    void shouldCollectReactiveItemFailuresAndKeepSubscriptionStateSeparate() {
        when(reactiveClient.bulk(any(BulkRequest.class))).thenAnswer(invocation ->
                Mono.defer(() -> Mono.just(response(invocation.getArgument(0), Set.of("1", "3")))));
        final var update = reactiveTemplate.bulkUpdate(updateQueries(),
                BulkOptions.builder().withMaxOperations(1).build(), index);

        for (var subscription = 0; subscription < 2; subscription++) {
            StepVerifier.create(update).expectErrorSatisfies(error ->
                    assertThat(error).isInstanceOfSatisfying(BulkFailureException.class,
                            exception -> assertThat(exception.getFailedDocuments()).containsOnlyKeys("1", "3")))
                    .verify();
        }
        assertThat(requests).hasSize(6);
    }

    @Test
    void shouldWaitForEachReactiveRequestBeforeSendingTheNext() {
        final var first = TestPublisher.<BulkResponse>create();
        final var second = TestPublisher.<BulkResponse>create();
        final var request = ArgumentCaptor.forClass(BulkRequest.class);
        when(reactiveClient.bulk(any(BulkRequest.class))).thenReturn(first.mono(), second.mono());

        StepVerifier.create(reactiveTemplate.bulkUpdate(updateQueries().subList(0, 2),
                BulkOptions.builder().withMaxOperations(1).build(), index))
                .then(() -> {
                    verify(reactiveClient).bulk(request.capture());
                    first.emit(response(request.getValue(), Set.of()));
                    verify(reactiveClient, times(2)).bulk(request.capture());
                    second.emit(response(request.getValue(), Set.of()));
                }).verifyComplete();
    }

    @Test
    void shouldStopReactiveRequestsAtTransportFailure() {
        when(reactiveClient.bulk(any(BulkRequest.class))).thenAnswer(invocation -> Mono.defer(() -> {
            if (requests.size() == 1) {
                return Mono.error(new IOException("connection lost"));
            }
            return Mono.just(response(invocation.getArgument(0), Set.of()));
        }));

        StepVerifier.create(reactiveTemplate.bulkUpdate(updateQueries(),
                BulkOptions.builder().withMaxOperations(1).build(), index))
                .expectErrorSatisfies(error -> assertThat(error).hasRootCauseMessage("connection lost")).verify();
        assertThat(requests).hasSize(1);
        verify(reactiveClient, times(2)).bulk(any(BulkRequest.class));
    }

    private List<org.springframework.data.elasticsearch.core.query.IndexQuery> indexQueries() {
        return List.of("1", "2", "3").stream().map(id -> new IndexQueryBuilder()
                .withId(id).withObject(Map.of("message", "test")).build()).toList();
    }

    private List<UpdateQuery> updateQueries() {
        return List.of("1", "2", "3").stream().map(id -> UpdateQuery.builder(id)
                .withDocument(Document.from(Map.of("message", "updated"))).build()).toList();
    }

    private BulkResponse response(final BulkRequest request, final Set<String> failedIds) {
        requests.add(request);
        final var items = request.operations().stream().map(operation -> {
            final var id = operation.isUpdate() ? operation.update().id() : operation.index().id();
            final var builder = new BulkResponseItem.Builder().operationType(
                    operation.isUpdate() ? OperationType.Update : OperationType.Index)
                    .index("index").id(id);
            if (failedIds.contains(id)) {
                builder.status(400).error(error -> error.type("test_failure").reason("failed " + id));
            } else {
                builder.status(200).seqNo(1L).primaryTerm(1L).version(1L);
            }
            return builder.build();
        }).toList();
        return BulkResponse.of(builder -> builder.items(items).took(1)
                .errors(items.stream().anyMatch(item -> item.error() != null)));
    }

}
