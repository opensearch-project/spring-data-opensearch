/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;

import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@SpringIntegrationTest
@ContextConfiguration(classes = { OpenSearchObservationIntegrationTests.Config.class })
@DisplayName("Using Imperative OpenSearch Client with Observations")
class OpenSearchObservationIntegrationTests {
    @Autowired private IndexNameProvider indexNameProvider;
    @Autowired private ElasticsearchOperations operations;
    @Autowired private ObservationRecorder recorder;
    private IndexOperations indexOperations;

    @Configuration
    @Import({ OpenSearchTemplateConfiguration.class })
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("integration-entity-observed-imperative-os");
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
        indexOperations.createWithMapping();
        recorder.clear();
    }

    @Test
    @Order(java.lang.Integer.MAX_VALUE)
    void cleanup() {
        operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + '*')).delete();
    }

    @AfterEach
    public void after() {
        recorder.clear();
    }

    @Test
    void imperativeDocumentsBulkSearchAndScroll() {
        operations.save(new Entity("1", "first"), IndexCoordinates.of(indexNameProvider.indexName()));
        assertThat(operations.get("1", Entity.class, IndexCoordinates.of(indexNameProvider.indexName()))).isNotNull();
        assertThat(operations.exists("1", IndexCoordinates.of(indexNameProvider.indexName()))).isTrue();
        operations.save(List.of(new Entity("2", "second"), new Entity("3", "third")), IndexCoordinates.of(indexNameProvider.indexName()));
        operations.bulkUpdate(List.of(UpdateQuery.builder("1").withDocument(Document.parse("{\"message\":\"changed\"}")).build()),
                BulkOptions.defaultOptions(), IndexCoordinates.of(indexNameProvider.indexName()));
        final var query = NativeQuery.builder().withQuery(q -> q.matchAll(m -> m)).build();
        assertThat(operations.count(query, Entity.class, IndexCoordinates.of(indexNameProvider.indexName()))).isEqualTo(3);
        assertThat(operations.search(query, Entity.class, IndexCoordinates.of(indexNameProvider.indexName())).getTotalHits()).isEqualTo(3);
        assertThat(operations.multiSearch(List.of(query, query), Entity.class, IndexCoordinates.of(indexNameProvider.indexName()))).hasSize(2);
        recorder.assertOperations(OpenSearchOperationName.SAVE, OpenSearchOperationName.GET, OpenSearchOperationName.EXISTS,
                OpenSearchOperationName.BULK, OpenSearchOperationName.BULK, OpenSearchOperationName.COUNT,
                OpenSearchOperationName.SEARCH, OpenSearchOperationName.MULTI_SEARCH);
        assertThat(recorder.stopped.get(3).getBatchSize()).isEqualTo(2);
        assertThat(recorder.stopped.getLast().getIndexName()).isEqualTo(indexNameProvider.indexName());
        assertThat(recorder.stopped).allSatisfy(context ->
                assertThat(context.getLowCardinalityKeyValues()).hasSize(1));
        recorder.clear();

        query.setPageable(PageRequest.of(0, 1));
        try (final var stream = operations.searchForStream(query, Entity.class, IndexCoordinates.of(indexNameProvider.indexName()))) {
            assertThat(stream.stream().count()).isEqualTo(3);
        }
        assertThat(recorder.started).extracting(OpenSearchObservationContext::getOperationName)
                .contains(OpenSearchOperationName.SCROLL_START, OpenSearchOperationName.SCROLL_CONTINUE, OpenSearchOperationName.SCROLL_CLEAR)
                .doesNotContain(OpenSearchOperationName.SEARCH);
        assertThat(recorder.stopped).hasSameSizeAs(recorder.started);
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command"));
    }

    @Test
    void indexClusterAndTemplateOperations() {
        assertThat(indexOperations.exists()).isTrue();
        indexOperations.putMapping(Document.parse("{\"properties\":{\"message\":{\"type\":\"text\"}}}"));
        assertThat(indexOperations.getMapping()).isNotEmpty();
        assertThat(indexOperations.getSettings()).isNotNull();
        indexOperations.refresh();
        assertThat(indexOperations.getInformation(IndexCoordinates.of(indexNameProvider.indexName()))).hasSize(1);
        assertThat(operations.cluster().health()).isNotNull();
        assertThat(((OpenSearchTemplate)operations).getClusterVersion()).isNotBlank();

        recorder.assertOperations(OpenSearchOperationName.INDEX_EXISTS, OpenSearchOperationName.INDEX_PUT_MAPPING,
                OpenSearchOperationName.INDEX_GET_MAPPING, OpenSearchOperationName.INDEX_GET_SETTINGS,
                OpenSearchOperationName.INDEX_REFRESH, OpenSearchOperationName.INDEX_GET_INFORMATION,
                OpenSearchOperationName.CLUSTER_HEALTH, OpenSearchOperationName.CLUSTER_INFO);
        recorder.clear();

        final var name = indexNameProvider.indexName() + "-template";
        try {
            assertThat(indexOperations.putTemplate(PutTemplateRequest.builder(name, name + "-*").build())).isTrue();
            assertThat(indexOperations.existsTemplate(new ExistsTemplateRequest(name))).isTrue();
            assertThat(indexOperations.getTemplate(new GetTemplateRequest(name))).isNotNull();
            assertThat(indexOperations.deleteTemplate(new DeleteTemplateRequest(name))).isTrue();
            assertThat(indexOperations.putIndexTemplate(PutIndexTemplateRequest.builder().withName(name).withIndexPatterns(name + "-*").build())).isTrue();
            assertThat(indexOperations.existsIndexTemplate(new ExistsIndexTemplateRequest(name))).isTrue();
            assertThat(indexOperations.getIndexTemplate(new GetIndexTemplateRequest(name))).hasSize(1);
            assertThat(indexOperations.deleteIndexTemplate(new DeleteIndexTemplateRequest(name))).isTrue();
            assertThat(indexOperations.putComponentTemplate(PutComponentTemplateRequest.builder().withName(name)
                    .withTemplateData(ComponentTemplateRequestData.builder().withSettings(new Settings()).build()).build())).isTrue();
            assertThat(indexOperations.existsComponentTemplate(new ExistsComponentTemplateRequest(name))).isTrue();
            assertThat(indexOperations.getComponentTemplate(new GetComponentTemplateRequest(name))).hasSize(1);
            assertThat(indexOperations.deleteComponentTemplate(new DeleteComponentTemplateRequest(name))).isTrue();
            recorder.assertOperations(OpenSearchOperationName.INDEX_PUT_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_TEMPLATE,
                    OpenSearchOperationName.INDEX_GET_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_TEMPLATE,
                    OpenSearchOperationName.INDEX_PUT_INDEX_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_INDEX_TEMPLATE,
                    OpenSearchOperationName.INDEX_GET_INDEX_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_INDEX_TEMPLATE,
                    OpenSearchOperationName.INDEX_PUT_COMPONENT_TEMPLATE, OpenSearchOperationName.INDEX_EXISTS_COMPONENT_TEMPLATE,
                    OpenSearchOperationName.INDEX_GET_COMPONENT_TEMPLATE, OpenSearchOperationName.INDEX_DELETE_COMPONENT_TEMPLATE);
            assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getIndexName()).isNull());
            assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command"));
        } finally {
            if (indexOperations.existsTemplate(new ExistsTemplateRequest(name))) {
                indexOperations.deleteTemplate(new DeleteTemplateRequest(name));
            }
            if (indexOperations.existsIndexTemplate(new ExistsIndexTemplateRequest(name))) {
                indexOperations.deleteIndexTemplate(new DeleteIndexTemplateRequest(name));
            }
            if (indexOperations.existsComponentTemplate(new ExistsComponentTemplateRequest(name))) {
                indexOperations.deleteComponentTemplate(new DeleteComponentTemplateRequest(name));
            }
        }
    }

    @Test
    void aliasLookupsTagOnlyExplicitIndices() {
        final var alias = indexNameProvider.indexName() + "-alias";
        final var actions = new AliasActions(new AliasAction.Add(AliasActionParameters.builder()
                .withIndices(indexNameProvider.indexName()).withAliases(alias).build()));
        assertThat(indexOperations.alias(actions)).isTrue();
        assertThat(indexOperations.getAliases(alias)).containsKey(indexNameProvider.indexName());
        assertThat(indexOperations.getAliasesForIndex(indexNameProvider.indexName())).containsKey(indexNameProvider.indexName());
        recorder.assertOperations(OpenSearchOperationName.INDEX_ALIAS, OpenSearchOperationName.INDEX_GET_ALIASES, OpenSearchOperationName.INDEX_GET_ALIASES);
        assertThat(recorder.stopped.get(0).getIndexName()).isNull();
        assertThat(recorder.stopped.get(1).getIndexName()).isNull();
        assertThat(recorder.stopped.get(2).getIndexName()).isEqualTo(indexNameProvider.indexName());
        assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command"));
    }

    @Test
    void scriptsPitAndReindex() {
        final var name = indexNameProvider.indexName() + "-script";
        final var target = IndexCoordinates.of(indexNameProvider.indexName() + "-target");
        var storedScriptExists = false;
        try {
            final var script = Script.builder().withId(name).withLanguage("painless").withSource("return 1").build();
            assertThat(operations.putScript(script)).isTrue();
            storedScriptExists = true;
            assertThat(operations.getScript(name)).isNotNull();
            assertThat(operations.deleteScript(name)).isTrue();
            storedScriptExists = false;
            final var pit = operations.openPointInTime(IndexCoordinates.of(indexNameProvider.indexName()), Duration.ofMinutes(1));
            assertThat(operations.closePointInTime(pit)).isTrue();
            final var request = ReindexRequest.builder(IndexCoordinates.of(indexNameProvider.indexName()), target).build();
            assertThat(operations.reindex(request)).isNotNull();
            assertThat(operations.submitReindex(request)).isNotBlank();
            recorder.assertOperations(OpenSearchOperationName.PUT_SCRIPT, OpenSearchOperationName.GET_SCRIPT,
                    OpenSearchOperationName.DELETE_SCRIPT, OpenSearchOperationName.OPEN_POINT_IN_TIME,
                    OpenSearchOperationName.CLOSE_POINT_IN_TIME, OpenSearchOperationName.REINDEX, OpenSearchOperationName.SUBMIT_REINDEX);
            assertThat(recorder.stopped)
                    .filteredOn(context -> context.getOperationName() == OpenSearchOperationName.REINDEX
                            || context.getOperationName() == OpenSearchOperationName.SUBMIT_REINDEX)
                    .hasSize(2).allSatisfy(context -> {
                        assertThat(context.getHighCardinalityKeyValue("spring.data.collection").getValue()).isEqualTo(indexNameProvider.indexName());
                        assertThat(context.getLowCardinalityKeyValue("spring.data.collection")).isNull();
                    });
            assertThat(recorder.stopped).allSatisfy(context -> assertThat(context.getName()).isEqualTo("spring.data.opensearch.command"));
        } finally {
            if (storedScriptExists) {
                operations.deleteScript(name);
            }
            operations.indexOps(target).delete();
        }
    }

    @org.springframework.data.elasticsearch.annotations.Document(indexName = "#{@indexNameProvider.indexName()}")
    record Entity(@Id String id, String message) {}
}
