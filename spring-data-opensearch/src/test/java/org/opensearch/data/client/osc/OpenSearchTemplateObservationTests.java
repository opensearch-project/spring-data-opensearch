/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.ExistsRequest;
import org.opensearch.client.opensearch.core.ReindexResponse;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.data.core.OpenSearchMappingParametersCustomizer;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.routing.RoutingResolver;
import org.springframework.data.elasticsearch.core.script.Script;

class OpenSearchTemplateObservationTests {
    private static final IndexCoordinates INDEX = IndexCoordinates.of("observed");
    private final OpenSearchClient client = mock(OpenSearchClient.class, RETURNS_DEEP_STUBS);
    private final ObservationRecorder recorder = new ObservationRecorder();
    private final MappingElasticsearchConverter converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
    private OpenSearchTemplate template;

    @BeforeEach
    void setUp() {
        when(client._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        template = new OpenSearchTemplate(client, converter, new OpenSearchMappingParametersCustomizer());
        template.setObservationRegistry(recorder.registry);
    }

    @Test
    void existsRecordsOneCommandAcrossClassOverloadAndCopies() throws IOException {
        when(client.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        assertThat(template.exists("1", Sample.class)).isTrue();
        template.withRouting(RoutingResolver.just("route")).exists("1", INDEX);
        template.withRefreshPolicy(RefreshPolicy.IMMEDIATE).exists("1", INDEX);
        recorder.assertOperations(OpenSearchOperationName.EXISTS, OpenSearchOperationName.EXISTS, OpenSearchOperationName.EXISTS);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command"));
    }

    @Test
    void discoversRegistryAndConventionAndKeepsThemInCopies() throws IOException {
        when(client.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        template = new OpenSearchTemplate(client, converter, new OpenSearchMappingParametersCustomizer());
        try (final var context = new StaticApplicationContext()) {
            context.getBeanFactory().registerSingleton("observationRegistry", recorder.registry);
            context.getBeanFactory().registerSingleton("convention", new CustomConvention());
            template.setApplicationContext(context);
            template.withRefreshPolicy(RefreshPolicy.IMMEDIATE).exists("1", INDEX);
            template.withRouting(RoutingResolver.just("route")).exists("1", INDEX);
        }
        recorder.assertOperations(OpenSearchOperationName.EXISTS, OpenSearchOperationName.EXISTS);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("custom.opensearch"));
    }

    @Test
    void explicitConfigurationWinsOverBeans() throws IOException {
        when(client.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        template.setObservationConvention(new CustomConvention());
        try (final var context = new StaticApplicationContext()) {
            context.getBeanFactory().registerSingleton("observationRegistry", ObservationRegistry.create());
            context.getBeanFactory().registerSingleton("convention", DefaultOpenSearchObservationConvention.INSTANCE);
            template.setApplicationContext(context);
            template.exists("1", INDEX);
        }
        recorder.assertOperations(OpenSearchOperationName.EXISTS);
        assertThat(recorder.stopped.getFirst().getName()).isEqualTo("custom.opensearch");
    }

    @Test
    void noopDoesNotChangeResult() throws IOException {
        when(client.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        template.setObservationRegistry(ObservationRegistry.NOOP);
        assertThat(template.exists("1", INDEX)).isTrue();
        recorder.assertOperations();
    }

    @Test
    void bulkArgumentValidationPrecedesBatchSizeAccess() {
        assertThatIllegalArgumentException().isThrownBy(() -> template.bulkOperation(null, BulkOptions.defaultOptions(), INDEX));
        recorder.assertOperations();
    }

    @Test
    void backgroundReindexStopsAfterSubmission() throws IOException {
        when(client.reindex(any(org.opensearch.client.opensearch.core.ReindexRequest.class)))
                .thenReturn(ReindexResponse.of(response -> response.task("node:1")));
        assertThat(template.submitReindex(reindex())).isEqualTo("node:1");
        recorder.assertOperations(OpenSearchOperationName.SUBMIT_REINDEX);
        assertThat(recorder.stopped.getFirst().getIndexName()).isEqualTo(INDEX.getIndexName());
        verify(client).reindex(argThat((org.opensearch.client.opensearch.core.ReindexRequest request) ->
                Boolean.FALSE.equals(request.waitForCompletion())));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void reindexFailuresRecordSourceIndicesOnlyInTraces(final boolean submission) {
        final var failure = new IllegalStateException("reindex failure");
        final var failing = new OpenSearchTemplate(client, converter, new OpenSearchMappingParametersCustomizer()) {
            @Override
            public <T> T execute(final ClientCallback<T> callback) {
                throw failure;
            }
        };
        failing.setObservationRegistry(recorder.registry);
        final var source = IndexCoordinates.of("source-one", "source-two");
        final var request = ReindexRequest.builder(source, IndexCoordinates.of("target")).build();

        assertThatThrownBy(() -> {
            if (submission) {
                failing.submitReindex(request);
            } else {
                failing.reindex(request);
            }
        }).isSameAs(failure);

        recorder.assertOperations(submission ? OpenSearchOperationName.SUBMIT_REINDEX : OpenSearchOperationName.REINDEX);
        final var context = recorder.stopped.getFirst();
        assertThat(context.getIndexCoordinates()).isSameAs(source);
        assertThat(context.getHighCardinalityKeyValue("spring.data.collection").getValue()).isEqualTo("source-one,source-two");
        assertThat(context.getLowCardinalityKeyValues()).hasSize(1);
        assertThat(context.getLowCardinalityKeyValue("spring.data.collection")).isNull();
        assertThat(context.getError()).isSameAs(failure);
    }

    @ParameterizedTest
    @MethodSource("operations")
    void recordsTranslatedFailuresOnce(final OpenSearchOperationName operation, final Consumer<OpenSearchTemplate> action) {
        final var failure = new IllegalStateException("client failure");
        final var failing = new OpenSearchTemplate(client, converter, new OpenSearchMappingParametersCustomizer()) {
            @Override
            public <T> T execute(final ClientCallback<T> callback) {
                throw failure;
            }
        };
        failing.setObservationRegistry(recorder.registry);
        assertThatThrownBy(() -> action.accept(failing)).isSameAs(failure);
        recorder.assertOperations(operation);
        assertThat(recorder.stopped.getFirst().getError()).isSameAs(failure);
    }

    static Stream<Arguments> operations() {
        final var query = NativeQuery.builder().withQuery(q -> q.matchAll(m -> m)).build();
        final var update = UpdateQuery.builder("1").withDocument(Document.from(java.util.Map.of("text", "updated"))).build();
        final var indexQuery = new IndexQueryBuilder().withId("1").withObject(new Sample("1")).build();
        return Stream.of(
                operation(OpenSearchOperationName.SAVE, t -> t.save(new Sample("1"), INDEX)),
                operation(OpenSearchOperationName.INDEX, t -> t.index(indexQuery, INDEX)),
                operation(OpenSearchOperationName.GET, t -> t.get("1", Sample.class, INDEX)),
                operation(OpenSearchOperationName.MULTI_GET, t -> t.multiGet(Query.multiGetQuery(List.of("1")), Sample.class, INDEX)),
                operation(OpenSearchOperationName.DELETE, t -> t.delete(new Sample("1"), INDEX)),
                operation(OpenSearchOperationName.DELETE_BY_QUERY, t -> t.delete(DeleteQuery.builder(query).build(), Sample.class, INDEX)),
                operation(OpenSearchOperationName.UPDATE, t -> t.update(update, INDEX)),
                operation(OpenSearchOperationName.UPDATE_BY_QUERY, t -> t.updateByQuery(UpdateQuery.builder(query).withScript("ctx._source.text = 'x'").withLang("painless").build(), INDEX)),
                operation(OpenSearchOperationName.BULK, t -> t.bulkUpdate(List.of(update), BulkOptions.defaultOptions(), INDEX)),
                operation(OpenSearchOperationName.BULK, t -> t.bulkIndex(List.of(indexQuery), INDEX)),
                operation(OpenSearchOperationName.COUNT, t -> t.count(query, Sample.class, INDEX)),
                operation(OpenSearchOperationName.SEARCH, t -> t.search(query, Sample.class, INDEX)),
                operation(OpenSearchOperationName.MULTI_SEARCH, t -> t.multiSearch(List.of(query), Sample.class, INDEX)),
                operation(OpenSearchOperationName.SCROLL_START, t -> t.searchScrollStart(1000, query, Sample.class, INDEX)),
                operation(OpenSearchOperationName.SCROLL_CONTINUE, t -> t.searchScrollContinue("scroll", 1000, Sample.class, INDEX)),
                operation(OpenSearchOperationName.SCROLL_CLEAR, t -> t.searchScrollClear(List.of("scroll"))),
                operation(OpenSearchOperationName.OPEN_POINT_IN_TIME, t -> t.openPointInTime(INDEX, Duration.ofMinutes(1))),
                operation(OpenSearchOperationName.CLOSE_POINT_IN_TIME, t -> t.closePointInTime("pit")),
                operation(OpenSearchOperationName.LIST_POINT_IN_TIME, OpenSearchTemplate::listPointInTime),
                operation(OpenSearchOperationName.REINDEX, t -> t.reindex(reindex())),
                operation(OpenSearchOperationName.SUBMIT_REINDEX, t -> t.submitReindex(reindex())),
                operation(OpenSearchOperationName.PUT_SCRIPT, t -> t.putScript(Script.builder().withId("script").withLanguage("painless").withSource("return 1").build())),
                operation(OpenSearchOperationName.GET_SCRIPT, t -> t.getScript("script")),
                operation(OpenSearchOperationName.DELETE_SCRIPT, t -> t.deleteScript("script")));
    }

    private static Arguments operation(final OpenSearchOperationName name, final Consumer<OpenSearchTemplate> action) {
        return Arguments.of(name, action);
    }

    static ReindexRequest reindex() {
        return ReindexRequest.builder(INDEX, IndexCoordinates.of("target")).build();
    }

    @org.springframework.data.elasticsearch.annotations.Document(indexName = "observed")
    static class Sample {
        @Id private final String id;

        Sample(final String id) {
            this.id = id;
        }
    }

    static class CustomConvention extends DefaultOpenSearchObservationConvention {
        @Override
        public String getName() {
            return "custom.opensearch";
        }
    }
}
