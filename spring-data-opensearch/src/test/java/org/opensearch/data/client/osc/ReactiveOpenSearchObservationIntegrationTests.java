/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.elasticsearch.core.IndexOperationsAdapter.blocking;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.junit.jupiter.ReactiveOpenSearchTemplateConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringIntegrationTest
@ContextConfiguration(classes = { ReactiveOpenSearchObservationIntegrationTests.Config.class })
@DisplayName("Using Reactive OpenSearch Client with Observations")
class ReactiveOpenSearchObservationIntegrationTests {
    @Autowired private IndexNameProvider indexNameProvider;
    @Autowired private ReactiveElasticsearchOperations operations;
    @Autowired private ObservationRecorder recorder;
    private ReactiveIndexOperations indexOperations;

    @Configuration
    @Import({ ReactiveOpenSearchTemplateConfiguration.class })
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("integration-entity-observed-reactive-os");
        }

        @Bean
        ObservationRecorder observationRecorder() {
            return new ObservationRecorder();
        }

        @Bean
        ObservationRegistry observationRegistry(ObservationRecorder recorder) {
            return recorder.registry;
        }
    }

    @BeforeEach
    public void before() {
        indexNameProvider.increment();
        indexOperations = operations.indexOps(Entity.class);
        StepVerifier.create(indexOperations.createWithMapping()).expectNextMatches(s -> true).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.INDEX_CREATE);
        recorder.clear();
    }

    @AfterEach
    public void after() {
        recorder.clear();
    }

    @Test
    @Order(java.lang.Integer.MAX_VALUE)
    void cleanup() {
        blocking(operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*'))).delete();
    }

    @Test
    void reactiveDocumentsAndResponseSearchesUseCallerParent() {
        final var parent = Observation.start("caller", recorder.registry);
        final var query = NativeQuery.builder().withQuery(q -> q.matchAll(m -> m)).build();
        StepVerifier.create(operations.save(new Entity("1", "first"), IndexCoordinates.of(indexNameProvider.indexName()))
                .thenMany(operations.saveAll(Mono.just(List.of(new Entity("2", "second"))), IndexCoordinates.of(indexNameProvider.indexName())))
                .then(operations.exists("1", IndexCoordinates.of(indexNameProvider.indexName())))
                .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, parent)))
                .expectNext(true).verifyComplete();
        parent.stop();
        recorder.assertOperations(OpenSearchOperationName.SAVE, OpenSearchOperationName.BULK, OpenSearchOperationName.EXISTS);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getParentObservation()).isSameAs(parent));
        recorder.clear();
        StepVerifier.create(operations.searchForPage(query, Entity.class, IndexCoordinates.of(indexNameProvider.indexName()))).expectNextCount(1).verifyComplete();
        StepVerifier.create(operations.searchForHits(query, Entity.class, IndexCoordinates.of(indexNameProvider.indexName()))).expectNextCount(1).verifyComplete();
        StepVerifier.create(operations.aggregate(query, Entity.class, IndexCoordinates.of(indexNameProvider.indexName()))).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.SEARCH, OpenSearchOperationName.SEARCH, OpenSearchOperationName.SEARCH);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command"));
    }

    @Test
    void reactiveIndexClusterAndPitOperations() {
        StepVerifier.create(indexOperations.exists()).expectNext(true).verifyComplete();
        StepVerifier.create(indexOperations.putMapping(Mono.just(Document.parse("{\"properties\":{\"message\":{\"type\":\"text\"}}}"))))
                .expectNext(true).verifyComplete();
        StepVerifier.create(indexOperations.getMapping()).expectNextCount(1).verifyComplete();
        StepVerifier.create(indexOperations.getSettings()).expectNextCount(1).verifyComplete();
        StepVerifier.create(indexOperations.refresh()).verifyComplete();
        StepVerifier.create(indexOperations.getInformation(IndexCoordinates.of(indexNameProvider.indexName()))).expectNextCount(1).verifyComplete();
        StepVerifier.create(operations.cluster().health()).expectNextCount(1).verifyComplete();
        StepVerifier.create(((ReactiveOpenSearchTemplate)operations).getClusterVersion()).expectNextCount(1).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.INDEX_EXISTS, OpenSearchOperationName.INDEX_PUT_MAPPING,
                OpenSearchOperationName.INDEX_GET_MAPPING, OpenSearchOperationName.INDEX_GET_SETTINGS,
                OpenSearchOperationName.INDEX_REFRESH, OpenSearchOperationName.INDEX_GET_INFORMATION,
                OpenSearchOperationName.CLUSTER_HEALTH, OpenSearchOperationName.CLUSTER_INFO);
        recorder.clear();
        StepVerifier.create(operations.openPointInTime(IndexCoordinates.of(indexNameProvider.indexName()), Duration.ofMinutes(1))
                .flatMap(operations::closePointInTime)).expectNext(true).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.OPEN_POINT_IN_TIME, OpenSearchOperationName.CLOSE_POINT_IN_TIME);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command"));
    }

    @Test
    void reactiveAliasLookupsTagOnlyExplicitIndices() {
        final var alias = indexNameProvider.indexName() + "-alias";
        final var actions = new AliasActions(new AliasAction.Add(AliasActionParameters.builder()
                .withIndices(indexNameProvider.indexName()).withAliases(alias).build()));
        StepVerifier.create(indexOperations.alias(actions)).expectNext(true).verifyComplete();
        StepVerifier.create(indexOperations.getAliases(alias)).expectNextCount(1).verifyComplete();
        StepVerifier.create(indexOperations.getAliasesForIndex(indexNameProvider.indexName())).expectNextCount(1).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.INDEX_ALIAS, OpenSearchOperationName.INDEX_GET_ALIASES, OpenSearchOperationName.INDEX_GET_ALIASES);
        assertThat(recorder.stopped.get(0).getIndexName()).isNull();
        assertThat(recorder.stopped.get(1).getIndexName()).isNull();
        assertThat(recorder.stopped.get(2).getIndexName()).isEqualTo(indexNameProvider.indexName());
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command"));
    }

    @Test
    void reactiveTemplateOperationsObserveOncePerPublicCall() {
        final var name = indexNameProvider.indexName() + "-reactive-template";

        StepVerifier.create(indexOperations.putTemplate(PutTemplateRequest.builder(name, name + "-*").build()))
                .expectNext(true).verifyComplete();
        StepVerifier.create(indexOperations.existsTemplate(new ExistsTemplateRequest(name))).expectNext(true).verifyComplete();
        StepVerifier.create(indexOperations.getTemplate(new GetTemplateRequest(name))).expectNextCount(1).verifyComplete();
        StepVerifier.create(indexOperations.deleteTemplate(new DeleteTemplateRequest(name))).expectNext(true).verifyComplete();
        StepVerifier.create(indexOperations.putIndexTemplate(PutIndexTemplateRequest.builder().withName(name).withIndexPatterns(name + "-*").build()))
                .expectNext(true).verifyComplete();
        StepVerifier.create(indexOperations.existsIndexTemplate(new ExistsIndexTemplateRequest(name))).expectNext(true).verifyComplete();
        StepVerifier.create(indexOperations.getIndexTemplate(new GetIndexTemplateRequest(name))).expectNextCount(1).verifyComplete();
        StepVerifier.create(indexOperations.deleteIndexTemplate(new DeleteIndexTemplateRequest(name))).expectNext(true).verifyComplete();
        StepVerifier.create(indexOperations.putComponentTemplate(PutComponentTemplateRequest.builder().withName(name)
                .withTemplateData(ComponentTemplateRequestData.builder().withSettings(new Settings()).build()).build()))
                .expectNext(true).verifyComplete();
        StepVerifier.create(indexOperations.existsComponentTemplate(new ExistsComponentTemplateRequest(name))).expectNext(true).verifyComplete();
        StepVerifier.create(indexOperations.getComponentTemplate(new GetComponentTemplateRequest(name))).expectNextCount(1).verifyComplete();
        StepVerifier.create(indexOperations.deleteComponentTemplate(new DeleteComponentTemplateRequest(name))).expectNext(true).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.INDEX_PUT_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_TEMPLATE,
                OpenSearchOperationName.INDEX_GET_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_TEMPLATE,
                OpenSearchOperationName.INDEX_PUT_INDEX_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_INDEX_TEMPLATE,
                OpenSearchOperationName.INDEX_GET_INDEX_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_INDEX_TEMPLATE,
                OpenSearchOperationName.INDEX_PUT_COMPONENT_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_COMPONENT_TEMPLATE,
                OpenSearchOperationName.INDEX_GET_COMPONENT_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_COMPONENT_TEMPLATE);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getIndexName()).isNull());
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command"));
    }

    @Test
    void scriptsPitAndReindex() {
        final var name = indexNameProvider.indexName() + "-script";
        final var target = IndexCoordinates.of(indexNameProvider.indexName() + "-target");

        final var script = Script.builder().withId(name).withLanguage("painless").withSource("return 1").build();
        StepVerifier.create(operations.putScript(script)).expectNext(true).verifyComplete();
        StepVerifier.create(operations.getScript(name)).expectNextCount(1).verifyComplete();
        StepVerifier.create(operations.deleteScript(name)).expectNext(true).verifyComplete();

        final var request = ReindexRequest.builder(IndexCoordinates.of(indexNameProvider.indexName()), target).build();
        StepVerifier.create(operations.reindex(request)).expectNextCount(1).verifyComplete();
        StepVerifier.create(operations.submitReindex(request)).expectNextCount(1).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.PUT_SCRIPT, OpenSearchOperationName.GET_SCRIPT,
                OpenSearchOperationName.DELETE_SCRIPT, OpenSearchOperationName.REINDEX, OpenSearchOperationName.SUBMIT_REINDEX);
        assertThat(recorder.stopped)
                .filteredOn(context -> context.getOperationName() == OpenSearchOperationName.REINDEX
                        || context.getOperationName() == OpenSearchOperationName.SUBMIT_REINDEX)
                .hasSize(2).allSatisfy(context -> {
                    assertThat(context.getHighCardinalityKeyValue("spring.data.collection").getValue()).isEqualTo(indexNameProvider.indexName());
                    assertThat(context.getLowCardinalityKeyValue("spring.data.collection")).isNull();
                });
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command"));
    }

    @org.springframework.data.elasticsearch.annotations.Document(indexName = "#{@indexNameProvider.indexName()}")
    record Entity(@Id String id, String message) {}
}
