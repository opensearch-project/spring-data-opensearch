/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;

import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.routing.RoutingResolver;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@SpringIntegrationTest
@ContextConfiguration(classes = { OpenSearchTemplateObservationIntegrationTests.Config.class })
@DisplayName("Using OpenSearch Client with Observations (CustomConvention)")
class OpenSearchTemplateObservationIntegrationTests {
    @Autowired private IndexNameProvider indexNameProvider;
    @Autowired private ElasticsearchOperations operations;
    @Autowired private ObservationRecorder recorder;
    private IndexOperations indexOperations;

    @Configuration
    @Import({ OpenSearchTemplateConfiguration.class })
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("integration-observed-os");
        }

        @Bean
        ObservationRecorder observationRecorder() {
            return new ObservationRecorder();
        }

        @Bean
        ObservationRegistry observationRegistry(ObservationRecorder recorder) {
            return recorder.registry;
        }

        @Bean
        OpenSearchObservationConvention convention() {
            return new CustomConvention();
        }
    }

    @BeforeEach
    public void before() {
        indexNameProvider.increment();
        indexOperations = operations.indexOps(Sample.class);
        indexOperations.createWithMapping();
        operations.index(IndexQuery.builder().withObject(new Sample("1")).build(), indexOperations.getIndexCoordinates());
        recorder.clear();
    }

    @AfterEach
    public void after() {
        recorder.clear();
    }

    @Test
    @Order(java.lang.Integer.MAX_VALUE)
    void cleanup() {
        operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*')).delete();
    }

    @Test
    void existsRecordsOneCommandAcrossClassOverloadAndCopies() throws IOException {
        assertThat(operations.exists("1", Sample.class)).isTrue();
        assertThat(operations.withRouting(RoutingResolver.just("route")).exists("1", Sample.class)).isTrue();
        assertThat(operations.withRefreshPolicy(RefreshPolicy.IMMEDIATE).exists("1", Sample.class)).isTrue();
        recorder.assertOperations(OpenSearchOperationName.EXISTS, OpenSearchOperationName.EXISTS, OpenSearchOperationName.EXISTS);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command.custom"));
    }

    @Test
    void discoversRegistryAndConventionAndKeepsThemInCopies() throws IOException {
        assertThat(operations.withRefreshPolicy(RefreshPolicy.IMMEDIATE).exists("1", Sample.class)).isTrue();
        assertThat(operations.withRouting(RoutingResolver.just("route")).exists("1", Sample.class)).isTrue();
        recorder.assertOperations(OpenSearchOperationName.EXISTS, OpenSearchOperationName.EXISTS);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command.custom"));
    }

    @Test
    void bulkArgumentValidationPrecedesBatchSizeAccess() {
        assertThatIllegalArgumentException().isThrownBy(() -> operations.bulkIndex(null, BulkOptions.defaultOptions(), Sample.class));
        recorder.assertOperations();
    }

    @Test
    void backgroundReindexStopsAfterSubmission() throws IOException {
        assertThat(operations.submitReindex(reindex(indexNameProvider.indexName()))).isNotBlank();
        recorder.assertOperations(OpenSearchOperationName.SUBMIT_REINDEX);
        assertThat(recorder.stopped.getFirst().getIndexName()).isEqualTo(indexNameProvider.indexName());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void reindexFailuresRecordSourceIndicesOnlyInTraces(final boolean submission) {
        final var source = IndexCoordinates.of("source-one", "source-two");
        final var request = ReindexRequest.builder(source, IndexCoordinates.of("target")).build();

        if (submission) {
            operations.submitReindex(request).isEmpty();
        } else {
            assertThatThrownBy(() -> operations.reindex(request)).isInstanceOf(NoSuchIndexException.class);
        }

        recorder.assertOperations(submission ? OpenSearchOperationName.SUBMIT_REINDEX : OpenSearchOperationName.REINDEX);
        final var context = recorder.stopped.getFirst();
        assertThat(context.getIndexCoordinates()).isSameAs(source);
        assertThat(context.getHighCardinalityKeyValue("spring.data.collection").getValue()).isEqualTo("source-one,source-two");
        assertThat(context.getLowCardinalityKeyValues()).hasSize(1);
        assertThat(context.getLowCardinalityKeyValue("spring.data.collection")).isNull();
        if (!submission) {
            assertThat(context.getError()).isInstanceOf(NoSuchIndexException.class);
        }
    }


    @ParameterizedTest
    @MethodSource("operations")
    void recordsTranslatedFailuresOnce(final OpenSearchOperationName operation, final BiConsumer<ElasticsearchOperations, IndexCoordinates> action) {
        action.accept(operations, IndexCoordinates.of(indexNameProvider.indexName()));
        recorder.assertOperations(operation);
    }

    static Stream<Arguments> operations() {
        final var query = NativeQuery.builder().withQuery(q -> q.matchAll(m -> m)).build();
        final var update = UpdateQuery.builder("1").withDocument(Document.from(java.util.Map.of("text", "updated"))).build();
        final var indexQuery = new IndexQueryBuilder().withId("1").withObject(new Sample("1")).build();
        return Stream.of(
                operation(OpenSearchOperationName.SAVE, (t, p) -> t.save(new Sample("1"))),
                operation(OpenSearchOperationName.INDEX, (t, p) -> t.index(indexQuery, p)),
                operation(OpenSearchOperationName.GET, (t, p) -> t.get("1", Sample.class)),
                operation(OpenSearchOperationName.MULTI_GET, (t, p) -> t.multiGet(Query.multiGetQuery(List.of("1")), Sample.class)),
                operation(OpenSearchOperationName.DELETE, (t, p) -> t.delete(new Sample("2"))),
                operation(OpenSearchOperationName.DELETE_BY_QUERY, (t, p) -> t.delete(DeleteQuery.builder(query).build(), Sample.class)),
                operation(OpenSearchOperationName.UPDATE, (t, p) -> t.update(update, p)),
                operation(OpenSearchOperationName.UPDATE_BY_QUERY, (t, p) -> t.updateByQuery(UpdateQuery.builder(query).withScript("ctx._source.text = 'x'").withLang("painless").build(), p)),
                operation(OpenSearchOperationName.BULK, (t, p) -> t.bulkUpdate(List.of(update), BulkOptions.defaultOptions(), p)),
                operation(OpenSearchOperationName.BULK, (t, p) -> t.bulkIndex(List.of(indexQuery), Sample.class)),
                operation(OpenSearchOperationName.COUNT, (t, p) -> t.count(query, Sample.class)),
                operation(OpenSearchOperationName.SEARCH, (t, p) -> t.search(query, Sample.class)),
                operation(OpenSearchOperationName.MULTI_SEARCH, (t, p) -> t.multiSearch(List.of(query), Sample.class)),
                operation(OpenSearchOperationName.SCROLL_START, (t, p) -> t.searchForStream(query, Sample.class)),
                operation(OpenSearchOperationName.OPEN_POINT_IN_TIME, (t, p) -> t.openPointInTime(p, Duration.ofMinutes(1))),
                operation(OpenSearchOperationName.REINDEX, (t, p) -> t.reindex(reindex(p.getIndexName()))),
                operation(OpenSearchOperationName.SUBMIT_REINDEX,  (t, p) -> t.submitReindex(reindex(p.getIndexName()))),
                operation(OpenSearchOperationName.PUT_SCRIPT, (t, p) -> t.putScript(Script.builder().withId("script").withLanguage("painless").withSource("return 1").build())),
                operation(OpenSearchOperationName.GET_SCRIPT, (t, p) -> t.getScript("script")),
                operation(OpenSearchOperationName.DELETE_SCRIPT, (t, p) -> t.deleteScript("script")));
    }

    private static Arguments operation(final OpenSearchOperationName name, final BiConsumer<ElasticsearchOperations, IndexCoordinates> action) {
        return Arguments.of(name, action);
    }


    private static ReindexRequest reindex(String indexName) {
        return ReindexRequest.builder(IndexCoordinates.of(indexName),
            IndexCoordinates.of(indexName + "-target")).build();
    }


    @org.springframework.data.elasticsearch.annotations.Document(indexName = "#{@indexNameProvider.indexName()}")
    static class Sample {
        @Id private final String id;

        Sample(final String id) {
            this.id = id;
        }
    }

    static class CustomConvention extends DefaultOpenSearchObservationConvention {
        @Override
        public String getName() {
            return "spring.data.opensearch.command.custom";
        }
    }
}
