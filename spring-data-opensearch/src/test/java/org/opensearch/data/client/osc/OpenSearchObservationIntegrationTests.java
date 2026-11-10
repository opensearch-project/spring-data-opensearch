/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.data.core.OpenSearchMappingParametersCustomizer;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.data.elasticsearch.junit.jupiter.ClusterConnectionInfo;
import org.springframework.data.elasticsearch.junit.jupiter.SpringDataElasticsearchExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Tag("integration-test")
@ExtendWith(SpringDataElasticsearchExtension.class)
class OpenSearchObservationIntegrationTests {
    private final ObservationRecorder recorder = new ObservationRecorder();
    private OpenSearchClient client;
    private OpenSearchTemplate template;
    private ReactiveOpenSearchTemplate reactiveTemplate;
    private IndexCoordinates index;

    @BeforeEach
    void setUp(final ClusterConnectionInfo connection) {
        client = OpenSearchClients.createImperative(ClientConfiguration.builder()
                .connectedTo(connection.getHost() + ":" + connection.getHttpPort()).build());
        final var converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
        converter.afterPropertiesSet();
        template = new OpenSearchTemplate(client, converter, new OpenSearchMappingParametersCustomizer());
        template.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
        reactiveTemplate = new ReactiveOpenSearchTemplate(new ReactiveOpenSearchClient(client._transport()), converter);
        reactiveTemplate.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
        index = IndexCoordinates.of("observation-" + UUID.randomUUID());
        template.indexOps(index).create();
        template.setObservationRegistry(recorder.registry);
        reactiveTemplate.setObservationRegistry(recorder.registry);
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (client != null) {
            try {
                if (template != null && index != null) {
                    template.indexOps(index).delete();
                }
            } finally {
                client._transport().close();
            }
        }
    }

    @Test
    void imperativeDocumentsBulkSearchAndScroll() {
        template.save(new Entity("1", "first"), index);
        assertThat(template.get("1", Entity.class, index)).isNotNull();
        assertThat(template.exists("1", index)).isTrue();
        template.save(List.of(new Entity("2", "second"), new Entity("3", "third")), index);
        template.bulkUpdate(List.of(UpdateQuery.builder("1").withDocument(Document.parse("{\"message\":\"changed\"}")).build()),
                BulkOptions.defaultOptions(), index);
        final var query = NativeQuery.builder().withQuery(q -> q.matchAll(m -> m)).build();
        assertThat(template.count(query, Entity.class, index)).isEqualTo(3);
        assertThat(template.search(query, Entity.class, index).getTotalHits()).isEqualTo(3);
        assertThat(template.multiSearch(List.of(query, query), Entity.class, index)).hasSize(2);
        recorder.assertOperations(OpenSearchOperationName.SAVE, OpenSearchOperationName.GET, OpenSearchOperationName.EXISTS,
                OpenSearchOperationName.BULK, OpenSearchOperationName.BULK, OpenSearchOperationName.COUNT,
                OpenSearchOperationName.SEARCH, OpenSearchOperationName.MULTI_SEARCH);
        assertThat(recorder.stopped.get(3).getBatchSize()).isEqualTo(2);
        assertThat(recorder.stopped.getLast().getIndexName()).isEqualTo(index.getIndexName());
        assertThat(recorder.stopped).allSatisfy(context ->
                assertThat(context.getLowCardinalityKeyValues()).hasSize(1));
        recorder.clear();

        query.setPageable(PageRequest.of(0, 1));
        try (final var stream = template.searchForStream(query, Entity.class, index)) {
            assertThat(stream.stream().count()).isEqualTo(3);
        }
        assertThat(recorder.started).extracting(OpenSearchObservationContext::getOperationName)
                .contains(OpenSearchOperationName.SCROLL_START, OpenSearchOperationName.SCROLL_CONTINUE, OpenSearchOperationName.SCROLL_CLEAR)
                .doesNotContain(OpenSearchOperationName.SEARCH);
        assertThat(recorder.stopped).hasSameSizeAs(recorder.started);
    }

    @Test
    void reactiveDocumentsAndResponseSearchesUseCallerParent() {
        final var parent = Observation.start("caller", recorder.registry);
        final var query = NativeQuery.builder().withQuery(q -> q.matchAll(m -> m)).build();
        StepVerifier.create(reactiveTemplate.save(new Entity("1", "first"), index)
                .thenMany(reactiveTemplate.saveAll(Mono.just(List.of(new Entity("2", "second"))), index))
                .then(reactiveTemplate.exists("1", index))
                .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, parent)))
                .expectNext(true).verifyComplete();
        parent.stop();
        recorder.assertOperations(OpenSearchOperationName.SAVE, OpenSearchOperationName.BULK, OpenSearchOperationName.EXISTS);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getParentObservation()).isSameAs(parent));
        recorder.clear();
        StepVerifier.create(reactiveTemplate.searchForPage(query, Entity.class, index)).expectNextCount(1).verifyComplete();
        StepVerifier.create(reactiveTemplate.searchForHits(query, Entity.class, index)).expectNextCount(1).verifyComplete();
        StepVerifier.create(reactiveTemplate.aggregate(query, Entity.class, index)).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.SEARCH, OpenSearchOperationName.SEARCH, OpenSearchOperationName.SEARCH);
    }

    @Test
    void indexClusterAndTemplateOperations() {
        final var indices = template.indexOps(index);
        assertThat(indices.exists()).isTrue();
        indices.putMapping(Document.parse("{\"properties\":{\"message\":{\"type\":\"text\"}}}"));
        assertThat(indices.getMapping()).isNotEmpty();
        assertThat(indices.getSettings()).isNotNull();
        indices.refresh();
        assertThat(indices.getInformation(index)).hasSize(1);
        assertThat(template.cluster().health()).isNotNull();
        assertThat(template.getClusterVersion()).isNotBlank();
        recorder.assertOperations(OpenSearchOperationName.INDEX_EXISTS, OpenSearchOperationName.INDEX_PUT_MAPPING,
                OpenSearchOperationName.INDEX_GET_MAPPING, OpenSearchOperationName.INDEX_GET_SETTINGS,
                OpenSearchOperationName.INDEX_REFRESH, OpenSearchOperationName.INDEX_GET_INFORMATION,
                OpenSearchOperationName.CLUSTER_HEALTH, OpenSearchOperationName.CLUSTER_INFO);
        recorder.clear();

        final var name = index.getIndexName() + "-template";
        try {
            assertThat(indices.putTemplate(PutTemplateRequest.builder(name, name + "-*").build())).isTrue();
            assertThat(indices.existsTemplate(new ExistsTemplateRequest(name))).isTrue();
            assertThat(indices.getTemplate(new GetTemplateRequest(name))).isNotNull();
            assertThat(indices.deleteTemplate(new DeleteTemplateRequest(name))).isTrue();
            assertThat(indices.putIndexTemplate(PutIndexTemplateRequest.builder().withName(name).withIndexPatterns(name + "-*").build())).isTrue();
            assertThat(indices.existsIndexTemplate(new ExistsIndexTemplateRequest(name))).isTrue();
            assertThat(indices.getIndexTemplate(new GetIndexTemplateRequest(name))).hasSize(1);
            assertThat(indices.deleteIndexTemplate(new DeleteIndexTemplateRequest(name))).isTrue();
            assertThat(indices.putComponentTemplate(PutComponentTemplateRequest.builder().withName(name)
                    .withTemplateData(ComponentTemplateRequestData.builder().withSettings(new Settings()).build()).build())).isTrue();
            assertThat(indices.existsComponentTemplate(new ExistsComponentTemplateRequest(name))).isTrue();
            assertThat(indices.getComponentTemplate(new GetComponentTemplateRequest(name))).hasSize(1);
            assertThat(indices.deleteComponentTemplate(new DeleteComponentTemplateRequest(name))).isTrue();
            recorder.assertOperations(OpenSearchOperationName.INDEX_PUT_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_TEMPLATE,
                    OpenSearchOperationName.INDEX_GET_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_TEMPLATE,
                    OpenSearchOperationName.INDEX_PUT_INDEX_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_INDEX_TEMPLATE,
                    OpenSearchOperationName.INDEX_GET_INDEX_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_INDEX_TEMPLATE,
                    OpenSearchOperationName.INDEX_PUT_COMPONENT_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_COMPONENT_TEMPLATE,
                    OpenSearchOperationName.INDEX_GET_COMPONENT_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_COMPONENT_TEMPLATE);
            assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getIndexName()).isNull());
        } finally {
            if (indices.existsTemplate(new ExistsTemplateRequest(name))) {
                indices.deleteTemplate(new DeleteTemplateRequest(name));
            }
            if (indices.existsIndexTemplate(new ExistsIndexTemplateRequest(name))) {
                indices.deleteIndexTemplate(new DeleteIndexTemplateRequest(name));
            }
            if (indices.existsComponentTemplate(new ExistsComponentTemplateRequest(name))) {
                indices.deleteComponentTemplate(new DeleteComponentTemplateRequest(name));
            }
        }
    }

    @Test
    void reactiveIndexClusterAndPitOperations() {
        final var indices = reactiveTemplate.indexOps(index);
        StepVerifier.create(indices.exists()).expectNext(true).verifyComplete();
        StepVerifier.create(indices.putMapping(Mono.just(Document.parse("{\"properties\":{\"message\":{\"type\":\"text\"}}}"))))
                .expectNext(true).verifyComplete();
        StepVerifier.create(indices.getMapping()).expectNextCount(1).verifyComplete();
        StepVerifier.create(indices.getSettings()).expectNextCount(1).verifyComplete();
        StepVerifier.create(indices.refresh()).verifyComplete();
        StepVerifier.create(indices.getInformation(index)).expectNextCount(1).verifyComplete();
        StepVerifier.create(reactiveTemplate.cluster().health()).expectNextCount(1).verifyComplete();
        StepVerifier.create(reactiveTemplate.getClusterVersion()).expectNextCount(1).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.INDEX_EXISTS, OpenSearchOperationName.INDEX_PUT_MAPPING,
                OpenSearchOperationName.INDEX_GET_MAPPING, OpenSearchOperationName.INDEX_GET_SETTINGS,
                OpenSearchOperationName.INDEX_REFRESH, OpenSearchOperationName.INDEX_GET_INFORMATION,
                OpenSearchOperationName.CLUSTER_HEALTH, OpenSearchOperationName.CLUSTER_INFO);
        recorder.clear();
        StepVerifier.create(reactiveTemplate.openPointInTime(index, Duration.ofMinutes(1))
                .flatMap(reactiveTemplate::closePointInTime)).expectNext(true).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.OPEN_POINT_IN_TIME, OpenSearchOperationName.CLOSE_POINT_IN_TIME);
    }

    @Test
    void aliasLookupsTagOnlyExplicitIndices() {
        final var alias = index.getIndexName() + "-alias";
        final var actions = new AliasActions(new AliasAction.Add(AliasActionParameters.builder()
                .withIndices(index.getIndexName()).withAliases(alias).build()));
        final var indices = template.indexOps(index);
        assertThat(indices.alias(actions)).isTrue();
        assertThat(indices.getAliases(alias)).containsKey(index.getIndexName());
        assertThat(indices.getAliasesForIndex(index.getIndexName())).containsKey(index.getIndexName());
        recorder.assertOperations(OpenSearchOperationName.INDEX_ALIAS, OpenSearchOperationName.INDEX_GET_ALIASES, OpenSearchOperationName.INDEX_GET_ALIASES);
        assertThat(recorder.stopped.get(0).getIndexName()).isNull();
        assertThat(recorder.stopped.get(1).getIndexName()).isNull();
        assertThat(recorder.stopped.get(2).getIndexName()).isEqualTo(index.getIndexName());
        recorder.clear();

        final var reactiveIndices = reactiveTemplate.indexOps(index);
        StepVerifier.create(reactiveIndices.alias(actions)).expectNext(true).verifyComplete();
        StepVerifier.create(reactiveIndices.getAliases(alias)).expectNextCount(1).verifyComplete();
        StepVerifier.create(reactiveIndices.getAliasesForIndex(index.getIndexName())).expectNextCount(1).verifyComplete();
        recorder.assertOperations(OpenSearchOperationName.INDEX_ALIAS, OpenSearchOperationName.INDEX_GET_ALIASES, OpenSearchOperationName.INDEX_GET_ALIASES);
        assertThat(recorder.stopped.get(0).getIndexName()).isNull();
        assertThat(recorder.stopped.get(1).getIndexName()).isNull();
        assertThat(recorder.stopped.get(2).getIndexName()).isEqualTo(index.getIndexName());
    }

    @Test
    void reactiveTemplateOperationsObserveOncePerPublicCall() {
        final var indices = reactiveTemplate.indexOps(index);
        final var name = index.getIndexName() + "-reactive-template";
        try {
            StepVerifier.create(indices.putTemplate(PutTemplateRequest.builder(name, name + "-*").build()))
                    .expectNext(true).verifyComplete();
            StepVerifier.create(indices.existsTemplate(new ExistsTemplateRequest(name))).expectNext(true).verifyComplete();
            StepVerifier.create(indices.getTemplate(new GetTemplateRequest(name))).expectNextCount(1).verifyComplete();
            StepVerifier.create(indices.deleteTemplate(new DeleteTemplateRequest(name))).expectNext(true).verifyComplete();
            StepVerifier.create(indices.putIndexTemplate(PutIndexTemplateRequest.builder().withName(name).withIndexPatterns(name + "-*").build()))
                    .expectNext(true).verifyComplete();
            StepVerifier.create(indices.existsIndexTemplate(new ExistsIndexTemplateRequest(name))).expectNext(true).verifyComplete();
            StepVerifier.create(indices.getIndexTemplate(new GetIndexTemplateRequest(name))).expectNextCount(1).verifyComplete();
            StepVerifier.create(indices.deleteIndexTemplate(new DeleteIndexTemplateRequest(name))).expectNext(true).verifyComplete();
            StepVerifier.create(indices.putComponentTemplate(PutComponentTemplateRequest.builder().withName(name)
                    .withTemplateData(ComponentTemplateRequestData.builder().withSettings(new Settings()).build()).build()))
                    .expectNext(true).verifyComplete();
            StepVerifier.create(indices.existsComponentTemplate(new ExistsComponentTemplateRequest(name))).expectNext(true).verifyComplete();
            StepVerifier.create(indices.getComponentTemplate(new GetComponentTemplateRequest(name))).expectNextCount(1).verifyComplete();
            StepVerifier.create(indices.deleteComponentTemplate(new DeleteComponentTemplateRequest(name))).expectNext(true).verifyComplete();
            recorder.assertOperations(OpenSearchOperationName.INDEX_PUT_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_TEMPLATE,
                    OpenSearchOperationName.INDEX_GET_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_TEMPLATE,
                    OpenSearchOperationName.INDEX_PUT_INDEX_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_INDEX_TEMPLATE,
                    OpenSearchOperationName.INDEX_GET_INDEX_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_INDEX_TEMPLATE,
                    OpenSearchOperationName.INDEX_PUT_COMPONENT_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_COMPONENT_TEMPLATE,
                    OpenSearchOperationName.INDEX_GET_COMPONENT_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_COMPONENT_TEMPLATE);
            assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getIndexName()).isNull());
        } finally {
            final var imperativeIndices = template.indexOps(index);
            if (imperativeIndices.existsTemplate(new ExistsTemplateRequest(name))) {
                imperativeIndices.deleteTemplate(new DeleteTemplateRequest(name));
            }
            if (imperativeIndices.existsIndexTemplate(new ExistsIndexTemplateRequest(name))) {
                imperativeIndices.deleteIndexTemplate(new DeleteIndexTemplateRequest(name));
            }
            if (imperativeIndices.existsComponentTemplate(new ExistsComponentTemplateRequest(name))) {
                imperativeIndices.deleteComponentTemplate(new DeleteComponentTemplateRequest(name));
            }
        }
    }

    @Test
    void scriptsPitAndReindex() {
        final var name = index.getIndexName() + "-script";
        final var target = IndexCoordinates.of(index.getIndexName() + "-target");
        var storedScriptExists = false;
        try {
            final var script = Script.builder().withId(name).withLanguage("painless").withSource("return 1").build();
            assertThat(template.putScript(script)).isTrue();
            storedScriptExists = true;
            assertThat(template.getScript(name)).isNotNull();
            assertThat(template.deleteScript(name)).isTrue();
            storedScriptExists = false;
            final var pit = template.openPointInTime(index, Duration.ofMinutes(1));
            assertThat(template.closePointInTime(pit)).isTrue();
            final var request = ReindexRequest.builder(index, target).build();
            assertThat(template.reindex(request)).isNotNull();
            assertThat(template.submitReindex(request)).isNotBlank();
            recorder.assertOperations(OpenSearchOperationName.PUT_SCRIPT, OpenSearchOperationName.GET_SCRIPT,
                    OpenSearchOperationName.DELETE_SCRIPT, OpenSearchOperationName.OPEN_POINT_IN_TIME,
                    OpenSearchOperationName.CLOSE_POINT_IN_TIME, OpenSearchOperationName.REINDEX, OpenSearchOperationName.SUBMIT_REINDEX);
            assertThat(recorder.stopped)
                    .filteredOn(context -> context.getOperationName() == OpenSearchOperationName.REINDEX
                            || context.getOperationName() == OpenSearchOperationName.SUBMIT_REINDEX)
                    .hasSize(2).allSatisfy(context -> {
                        assertThat(context.getHighCardinalityKeyValue("spring.data.collection").getValue()).isEqualTo(index.getIndexName());
                        assertThat(context.getLowCardinalityKeyValue("spring.data.collection")).isNull();
                    });
            recorder.clear();
            StepVerifier.create(reactiveTemplate.putScript(script)).expectNext(true).verifyComplete();
            storedScriptExists = true;
            StepVerifier.create(reactiveTemplate.getScript(name)).expectNextCount(1).verifyComplete();
            StepVerifier.create(reactiveTemplate.deleteScript(name)).expectNext(true).verifyComplete();
            storedScriptExists = false;
            StepVerifier.create(reactiveTemplate.reindex(request)).expectNextCount(1).verifyComplete();
            StepVerifier.create(reactiveTemplate.submitReindex(request)).expectNextCount(1).verifyComplete();
            recorder.assertOperations(OpenSearchOperationName.PUT_SCRIPT, OpenSearchOperationName.GET_SCRIPT,
                    OpenSearchOperationName.DELETE_SCRIPT, OpenSearchOperationName.REINDEX, OpenSearchOperationName.SUBMIT_REINDEX);
            assertThat(recorder.stopped)
                    .filteredOn(context -> context.getOperationName() == OpenSearchOperationName.REINDEX
                            || context.getOperationName() == OpenSearchOperationName.SUBMIT_REINDEX)
                    .hasSize(2).allSatisfy(context -> {
                        assertThat(context.getHighCardinalityKeyValue("spring.data.collection").getValue()).isEqualTo(index.getIndexName());
                        assertThat(context.getLowCardinalityKeyValue("spring.data.collection")).isNull();
                    });
        } finally {
            if (storedScriptExists) {
                template.deleteScript(name);
            }
            template.indexOps(target).delete();
        }
    }

    @org.springframework.data.elasticsearch.annotations.Document(indexName = "unused-observation-entity")
    record Entity(@Id String id, String message) {}
}
