/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.osc;

import static org.opensearch.client.util.ApiTypeHelper.*;
import static org.opensearch.data.client.osc.TypeUtils.*;

import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch._types.Result;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.CreatePitResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.transport.Version;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.client.UnsupportedBackendOperation;
import org.springframework.data.elasticsearch.core.AbstractReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.AggregationContainer;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.cluster.ReactiveClusterOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.data.elasticsearch.core.sql.SqlResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * Implementation of {@link org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations} using the new
 * OpenSearch client.
 */
public class ReactiveOpenSearchTemplate extends AbstractReactiveElasticsearchTemplate {

    private static final Log LOGGER = LogFactory.getLog(ReactiveOpenSearchTemplate.class);

    @Nullable private OpenSearchObservationConvention observationConvention;

    private final ReactiveOpenSearchClient client;
    private final RequestConverter requestConverter;
    private final ResponseConverter responseConverter;
    private final JsonpMapper jsonpMapper;
    private final OpenSearchExceptionTranslator exceptionTranslator;

    public ReactiveOpenSearchTemplate(ReactiveOpenSearchClient client, ElasticsearchConverter converter) {
        super(converter);

        Assert.notNull(client, "client must not be null");

        this.client = client;
        this.jsonpMapper = client._transport().jsonpMapper();
        requestConverter = new RequestConverter(converter, jsonpMapper);
        responseConverter = new ResponseConverter(jsonpMapper);
        exceptionTranslator = new OpenSearchExceptionTranslator(jsonpMapper);
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        if (observationRegistry == ObservationRegistry.NOOP) {
            applicationContext.getBeanProvider(ObservationRegistry.class).ifAvailable(this::setObservationRegistry);
        }
        if (observationConvention == null) {
            applicationContext.getBeanProvider(OpenSearchObservationConvention.class).ifAvailable(this::setObservationConvention);
        }
    }

    /**
     * Set a custom convention. A {@literal null} value restores the default convention.
     * Template copies retain this configuration.
     *
     * @param observationConvention the custom convention, or {@literal null}
     */
    public void setObservationConvention(@Nullable final OpenSearchObservationConvention observationConvention) {
        this.observationConvention = observationConvention;
    }

    private OpenSearchObservationSupport observations() {
        return new OpenSearchObservationSupport(observationRegistry, observationConvention);
    }

    @Override
    protected void customizeCopy(final AbstractReactiveElasticsearchTemplate copy) {
        if (copy instanceof ReactiveOpenSearchTemplate template) {
            template.observationConvention = observationConvention;
        }
    }

    // region Document operations
    @Override
    public <T> Mono<T> save(final T entity, final IndexCoordinates index) {
        Assert.notNull(entity, "Entity must not be null!");
        Assert.notNull(index, "index must not be null");
        return observations().mono(OpenSearchOperationName.SAVE, index, () -> super.save(entity, index));
    }

    @Override
    public Mono<Boolean> exists(final String id, final IndexCoordinates index) {
        Assert.notNull(id, "id must not be null");
        Assert.notNull(index, "index must not be null");
        return observations().mono(OpenSearchOperationName.EXISTS, index, () -> super.exists(id, index));
    }

    @Override
    public Mono<String> delete(final Object entity, final IndexCoordinates index) {
        return observations().mono(OpenSearchOperationName.DELETE, index, () -> super.delete(entity, index));
    }

    @Override
    public Mono<String> delete(final String id, final IndexCoordinates index) {
        Assert.notNull(id, "id must not be null");
        Assert.notNull(index, "index must not be null");
        return observations().mono(OpenSearchOperationName.DELETE, index, () -> super.delete(id, index));
    }

    @Override
    public <T> Flux<SearchHit<T>> search(final Query query, final Class<?> entityType, final Class<T> resultType,
            final IndexCoordinates index) {
        Assert.notNull(query, "query must not be null");
        Assert.notNull(entityType, "clazz must not be null");
        Assert.notNull(index, "index must not be null");
        return observations().flux(OpenSearchOperationName.SEARCH, index, null, () -> super.search(query, entityType, resultType, index));
    }

    @Override
    public Mono<Long> count(final Query query, final Class<?> entityType, final IndexCoordinates index) {
        Assert.notNull(query, "query must not be null");
        Assert.notNull(index, "index must not be null");
        return observations().mono(OpenSearchOperationName.COUNT, index, () -> super.count(query, entityType, index));
    }

    @Override
    protected <T> Mono<Tuple2<T, IndexResponseMetaData>> doIndex(T entity, IndexCoordinates index) {

        IndexRequest<?> indexRequest = requestConverter.documentIndexRequest(getIndexQuery(entity), index,
                getRefreshPolicy());
        return Mono.just(entity) //
                .zipWith(//
                        Mono.from(execute(client -> client.index(indexRequest))) //
                                .map(indexResponse -> new IndexResponseMetaData(indexResponse.id(), //
                                        indexResponse.index(), //
                                        indexResponse.seqNo(), //
                                        indexResponse.primaryTerm(), //
                                        indexResponse.version() //
                                )));
    }

    @Override
    public <T> Flux<T> saveAll(final Mono<? extends Collection<? extends T>> entitiesPublisher, final IndexCoordinates index) {

        Assert.notNull(entitiesPublisher, "entitiesPublisher must not be null!");

        return entitiesPublisher.flatMapMany(entities -> observations().flux(OpenSearchOperationName.BULK, index,
                entities.size(), () -> saveAllEntities(entities, index)));
    }

    private <T> Flux<T> saveAllEntities(final Collection<? extends T> entityCollection, final IndexCoordinates index) {
        return Flux.fromIterable(entityCollection)
                .concatMap(entity -> maybeCallbackBeforeConvert(entity, index))
                .collectList() //
                .map(Entities::new) //
                .flatMapMany(entities -> {

                    if (entities.isEmpty()) {
                        return Flux.empty();
                    }

                    return doBulkOperation(entities.indexQueries(), BulkOptions.defaultOptions(), index)//
                            .index() //
                            .flatMap(indexAndResponse -> {
                                final var savedEntity = entities.entityAt(indexAndResponse.getT1());
                                final var response = indexAndResponse.getT2();
                                final var updatedEntity = entityOperations.updateIndexedObject(
                                        savedEntity, new IndexedObjectInformation( //
                                                response.id(), //
                                                response.index(), //
                                                response.seqNo(), //
                                                response.primaryTerm(), //
                                                response.version()),
                                        converter,
                                        routingResolver);
                                return maybeCallbackAfterSave(updatedEntity, index);
                            });
                });
    }

    @Override
    protected Mono<Boolean> doExists(String id, IndexCoordinates index) {

        Assert.notNull(id, "id must not be null");
        Assert.notNull(index, "index must not be null");

        ExistsRequest existsRequest = requestConverter.documentExistsRequest(id, routingResolver.getRouting(), index);

        return Mono.from(execute(
                ((ClientCallback<Publisher<BooleanResponse>>) client -> client.exists(existsRequest))))
                .map(BooleanResponse::value) //
                .onErrorReturn(NoSuchIndexException.class, false);
    }

    @Override
    public Mono<ByQueryResponse> delete(final DeleteQuery query, final Class<?> entityType, final IndexCoordinates index) {
        Assert.notNull(query, "query must not be null");

        return observations().mono(OpenSearchOperationName.DELETE_BY_QUERY, index, () -> {
            final var request = requestConverter.documentDeleteByQueryRequest(query, routingResolver.getRouting(),
                    entityType, index, getRefreshPolicy());
            return Mono.from(execute(client -> client.deleteByQuery(request))).map(responseConverter::byQueryResponse);
        });
    }

    @Override
    public <T> Mono<T> get(final String id, final Class<T> entityType, final IndexCoordinates index) {

        Assert.notNull(id, "id must not be null");
        Assert.notNull(entityType, "entityType must not be null");
        Assert.notNull(index, "index must not be null");

        return observations().mono(OpenSearchOperationName.GET, index, () -> {
            final var getRequest = requestConverter.documentGetRequest(id, routingResolver.getRouting(), index);

            final var getResponse = Mono
                    .from(execute(client -> client.get(getRequest, EntityAsMap.class)));

            final var callback = new ReadDocumentCallback<>(converter, entityType, index);
            return getResponse.flatMap(response -> callback.toEntity(DocumentAdapters.from(response)));
        });
    }

    @Override
    public Mono<ReindexResponse> reindex(final ReindexRequest reindexRequest) {

        Assert.notNull(reindexRequest, "reindexRequest must not be null");

        return observations().mono(OpenSearchOperationName.REINDEX, reindexRequest.getSource().getIndexes(), () -> {
            final var reindexRequestES = requestConverter.reindex(reindexRequest,
                    true);

            return Mono.from(execute( //
                    client -> client.reindex(reindexRequestES))).map(responseConverter::reindexResponse);
        });
    }

    @Override
    public Mono<String> submitReindex(final ReindexRequest reindexRequest) {

        Assert.notNull(reindexRequest, "reindexRequest must not be null");

        return observations().mono(OpenSearchOperationName.SUBMIT_REINDEX, reindexRequest.getSource().getIndexes(), () -> {
            final var reindexRequestES = requestConverter.reindex(reindexRequest,
                    false);

            return Mono.from(execute( //
                    client -> client.reindex(reindexRequestES)))
                    .flatMap(response -> (response.task() == null)
                            ? Mono.error(
                                    new UnsupportedBackendOperation("ElasticsearchClient did not return a task id on submit request"))
                            : Mono.just(response.task()));
        });
    }

    @Override
    public Mono<UpdateResponse> update(final UpdateQuery updateQuery, final IndexCoordinates index) {

        Assert.notNull(updateQuery, "UpdateQuery must not be null");
        Assert.notNull(index, "Index must not be null");

        return observations().mono(OpenSearchOperationName.UPDATE, index, () -> {
            final var request = requestConverter.documentUpdateRequest(updateQuery, index, getRefreshPolicy(),
                    routingResolver.getRouting());

            return Mono.from(execute(client -> client.update(request, Document.class))).flatMap(response -> {
                final var result = result(response.result());
                return result == null ? Mono.empty() : Mono.just(UpdateResponse.of(result));
            });
        });
    }

    @Override
    public Mono<ByQueryResponse> updateByQuery(UpdateQuery updateQuery, IndexCoordinates index) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Mono<Void> bulkUpdate(final List<UpdateQuery> queries, final BulkOptions bulkOptions, final IndexCoordinates index) {

        Assert.notNull(queries, "List of UpdateQuery must not be null");
        Assert.notNull(bulkOptions, "BulkOptions must not be null");
        Assert.notNull(index, "Index must not be null");

        return observations().flux(OpenSearchOperationName.BULK, index, queries.size(),
                () -> doBulkOperation(queries, bulkOptions, index)).then();
    }

    private Flux<BulkResponseItem> doBulkOperation(List<?> queries, BulkOptions bulkOptions, IndexCoordinates index) {

        BulkRequest bulkRequest = requestConverter.documentBulkRequest(queries, bulkOptions, index, getRefreshPolicy());
        return client.bulk(bulkRequest)
                .onErrorMap(e -> new UncategorizedElasticsearchException("Error executing bulk request", e))
                .flatMap(this::checkForBulkOperationFailure) //
                .flatMapMany(response -> Flux.fromIterable(response.items()));

    }

    private Mono<BulkResponse> checkForBulkOperationFailure(BulkResponse bulkResponse) {

        if (bulkResponse.errors()) {
            Map<String, BulkFailureException.FailureDetails> failedDocuments = new HashMap<>();

            for (BulkResponseItem item : bulkResponse.items()) {

                if (item.error() != null) {
                    failedDocuments.put(item.id(), new BulkFailureException.FailureDetails(item.status(), item.error().reason()));
                }
            }
            BulkFailureException exception = new BulkFailureException(
                    "Bulk operation has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
                            + failedDocuments + ']',
                    failedDocuments);
            return Mono.error(exception);
        } else {
            return Mono.just(bulkResponse);
        }
    }

    @Override
    protected Mono<String> doDeleteById(String id, @Nullable String routing, IndexCoordinates index) {

        Assert.notNull(id, "id must not be null");
        Assert.notNull(index, "index must not be null");

        return Mono.defer(() -> {
            DeleteRequest deleteRequest = requestConverter.documentDeleteRequest(id, routing, index, getRefreshPolicy());
            return doDelete(deleteRequest);
        });
    }

    private Mono<String> doDelete(DeleteRequest request) {

        return Mono.from(execute(client -> client.delete(request))) //
                .flatMap(deleteResponse -> {
                    if (deleteResponse.result() == Result.NotFound) {
                        return Mono.empty();
                    }
                    return Mono.just(deleteResponse.id());
                }).onErrorResume(NoSuchIndexException.class, it -> Mono.empty());
    }

    @Override
    public <T> Flux<MultiGetItem<T>> multiGet(final Query query, final Class<T> clazz, final IndexCoordinates index) {

        Assert.notNull(query, "query must not be null");
        Assert.notNull(clazz, "clazz must not be null");

        return observations().flux(OpenSearchOperationName.MULTI_GET, index, null, () -> {
            final var request = requestConverter.documentMgetRequest(query, clazz, index);

            final var callback = new ReadDocumentCallback<>(converter, clazz, index);

            final var response = execute(client -> client.mget(request, EntityAsMap.class));

            return Mono.from(response)//
                    .flatMapMany(it -> Flux.fromIterable(DocumentAdapters.from(it))) //
                    .flatMap(multiGetItem -> {
                        if (multiGetItem.isFailed()) {
                            return Mono.just(MultiGetItem.of(null, multiGetItem.getFailure()));
                        } else {
                            return callback.toEntity(multiGetItem.getItem()) //
                                    .map(t -> MultiGetItem.of(t, multiGetItem.getFailure()));
                        }
                    });
        });
    }

    // endregion

    @Override
    protected ReactiveOpenSearchTemplate doCopy() {
        return new ReactiveOpenSearchTemplate(client, converter);
    }

    // region search operations

    @Override
    protected Flux<SearchDocument> doFind(Query query, Class<?> clazz, IndexCoordinates index) {

        Assert.notNull(query, "query must not be null");
        Assert.notNull(clazz, "clazz must not be null");
        Assert.notNull(index, "index must not be null");

        if (query instanceof SearchTemplateQuery searchTemplateQuery) {
            return Flux.defer(() -> doSearch(searchTemplateQuery, clazz, index));
        } else {
            return Flux.defer(() -> {
                boolean queryIsUnbounded = !(query.getPageable().isPaged() || query.isLimiting());
                return queryIsUnbounded ? doFindUnbounded(query, clazz, index) : doFindBounded(query, clazz, index);
            });
        }
    }

    private Flux<SearchDocument> doFindUnbounded(Query query, Class<?> clazz, IndexCoordinates index) {

        if (query instanceof BaseQuery baseQuery) {
            var pitKeepAlive = Duration.ofMinutes(5);
            // setup functions for Flux.usingWhen()
            Mono<PitSearchAfter> resourceSupplier = openPointInTime(index, pitKeepAlive, true)
                    .map(pit -> new PitSearchAfter(baseQuery, pit));

            Function<PitSearchAfter, Publisher<?>> asyncComplete = this::cleanupPit;

            BiFunction<PitSearchAfter, Throwable, Publisher<?>> asyncError = (psa, ex) -> {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Error during pit/search_after", ex);
                }
                return cleanupPit(psa);
            };

            Function<PitSearchAfter, Publisher<?>> asyncCancel = psa -> {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("pit/search_after was cancelled");
                }
                return cleanupPit(psa);
            };

            Function<PitSearchAfter, Publisher<? extends SearchResponse<EntityAsMap>>> resourceClosure = psa -> {

                baseQuery.setPointInTime(new Query.PointInTime(psa.getPit(), pitKeepAlive));

                // only add _shard_doc if there is not a field_collapse and a sort with the same name
                boolean addShardDoc = true;

                if (query instanceof NativeQuery nativeQuery && nativeQuery.getFieldCollapse() != null) {
                    var field = nativeQuery.getFieldCollapse().field();

                    if (nativeQuery.getSortOptions().stream()
                            .anyMatch(sortOptions -> sortOptions.isField() && sortOptions.field().field().equals(field))) {
                        addShardDoc = false;
                    }

                    if (query.getSort() != null
                            && query.getSort().stream().anyMatch(order -> order.getProperty().equals(field))) {
                        addShardDoc = false;
                    }
                }

                if (addShardDoc) {
                    baseQuery.addSort(Sort.by("_shard_doc"));
                }

                SearchRequest firstSearchRequest = requestConverter.searchRequest(baseQuery, routingResolver.getRouting(),
                        clazz, index, false, true);

                return Mono.from(execute(client -> client.search(firstSearchRequest, EntityAsMap.class)))
                        .expand(entityAsMapSearchResponse -> {

                            var hits = entityAsMapSearchResponse.hits().hits();
                            if (CollectionUtils.isEmpty(hits)) {
                                return Mono.empty();
                            }

                            List<Object> sortOptions = hits.get(hits.size() - 1).sort().stream().map(TypeUtils::toObject)
                                    .collect(Collectors.toList());
                            baseQuery.setSearchAfter(sortOptions);
                            SearchRequest followSearchRequest = requestConverter.searchRequest(baseQuery,
                                    routingResolver.getRouting(), clazz, index, false, true);
                            return Mono.from(execute(client -> client.search(followSearchRequest, EntityAsMap.class)));
                        });

            };

            Flux<SearchResponse<EntityAsMap>> searchResponses = Flux.usingWhen(resourceSupplier, resourceClosure, asyncComplete,
                    asyncError, asyncCancel);
            return searchResponses.flatMapIterable(entityAsMapSearchResponse -> entityAsMapSearchResponse.hits().hits())
                    .map(entityAsMapHit -> DocumentAdapters.from(entityAsMapHit, jsonpMapper));
        } else {
            return Flux.error(new IllegalArgumentException("Query must be derived from BaseQuery"));
        }
    }

    private Publisher<?> cleanupPit(PitSearchAfter psa) {
        var baseQuery = psa.getBaseQuery();
        baseQuery.setPointInTime(null);
        baseQuery.setSearchAfter(null);
        baseQuery.setSort(psa.getSort());
        var pit = psa.getPit();
        return StringUtils.hasText(pit) ? closePointInTime(pit) : Mono.empty();
    }

    static private class PitSearchAfter {
        private final BaseQuery baseQuery;
        @Nullable private final Sort sort;
        private final String pit;

        PitSearchAfter(BaseQuery baseQuery, String pit) {
            this.baseQuery = baseQuery;
            this.sort = baseQuery.getSort();
            this.pit = pit;
        }

        public BaseQuery getBaseQuery() {
            return baseQuery;
        }

        @Nullable
        public Sort getSort() {
            return sort;
        }

        public String getPit() {
            return pit;
        }
    }

    @Override
    protected Mono<Long> doCount(Query query, Class<?> entityType, IndexCoordinates index) {

        Assert.notNull(query, "query must not be null");
        Assert.notNull(index, "index must not be null");

        SearchRequest searchRequest = requestConverter.searchRequest(query, routingResolver.getRouting(), entityType, index,
                true);

        return Mono.from(execute(client -> client.search(searchRequest, EntityAsMap.class)))
                .map(searchResponse -> searchResponse.hits().total() != null ? searchResponse.hits().total().value() : 0L);
    }

    private Flux<SearchDocument> doFindBounded(Query query, Class<?> clazz, IndexCoordinates index) {

        SearchRequest searchRequest = requestConverter.searchRequest(query, routingResolver.getRouting(), clazz, index,
                false, false);

        return Mono.from(execute(client -> client.search(searchRequest, EntityAsMap.class))) //
                .flatMapIterable(entityAsMapSearchResponse -> entityAsMapSearchResponse.hits().hits()) //
                .map(entityAsMapHit -> DocumentAdapters.from(entityAsMapHit, jsonpMapper));
    }

    private Flux<SearchDocument> doSearch(SearchTemplateQuery query, Class<?> clazz, IndexCoordinates index) {

        var request = requestConverter.searchTemplate(query, routingResolver.getRouting(), index);

        return Mono.from(execute(client -> client.searchTemplate(request, EntityAsMap.class))) //
                .flatMapIterable(entityAsMapSearchResponse -> entityAsMapSearchResponse.hits().hits()) //
                .map(entityAsMapHit -> DocumentAdapters.from(entityAsMapHit, jsonpMapper));
    }

    @Override
    protected <T> Mono<SearchDocumentResponse> doFindForResponse(final Query query, final Class<?> clazz, final IndexCoordinates index) {

        Assert.notNull(query, "query must not be null");
        Assert.notNull(index, "index must not be null");

        return observations().mono(OpenSearchOperationName.SEARCH, index, () -> {
            final var searchRequest = requestConverter.searchRequest(query, routingResolver.getRouting(), clazz, index,
                    false);

            // noinspection unchecked
            final var callback = new ReadSearchDocumentCallback<>((Class<T>) clazz, index);
            final SearchDocumentResponse.EntityCreator<T> entityCreator = searchDocument -> callback.toEntity(searchDocument)
                    .toFuture();

            return Mono.from(execute(client -> client.search(searchRequest, EntityAsMap.class)))
                    .map(searchResponse -> SearchDocumentResponseBuilder.from(searchResponse, entityCreator, jsonpMapper));
        });
    }

    @Override
    public Flux<? extends AggregationContainer<?>> aggregate(Query query, Class<?> entityType, IndexCoordinates index) {

        return doFindForResponse(query, entityType, index).flatMapMany(searchDocumentResponse -> {
            OpenSearchAggregations aggregations = (OpenSearchAggregations) searchDocumentResponse.getAggregations();
            return aggregations == null ? Flux.empty() : Flux.fromIterable(aggregations.aggregations());
        });
    }

    @Override
    public Mono<String> openPointInTime(final IndexCoordinates index, final Duration keepAlive, final Boolean ignoreUnavailable) {

        Assert.notNull(index, "index must not be null");
        Assert.notNull(keepAlive, "keepAlive must not be null");
        Assert.notNull(ignoreUnavailable, "ignoreUnavailable must not be null");

        return observations().mono(OpenSearchOperationName.OPEN_POINT_IN_TIME, index, () -> {
            final var request = requestConverter.searchOpenPointInTimeRequest(index, keepAlive, ignoreUnavailable);
            return Mono.from(execute(client -> client.openPointInTime(request))).map(CreatePitResponse::pitId);
        });
    }

    @Override
    public Mono<Boolean> closePointInTime(final String pit) {

        Assert.notNull(pit, "pit must not be null");

        return observations().mono(OpenSearchOperationName.CLOSE_POINT_IN_TIME, null, () -> {
            final var request = requestConverter.searchClosePointInTime(pit);
            return Mono.from(execute(client -> client.closePointInTime(request))).map(r -> !r.pits().isEmpty());
        });
    }

    // endregion

    // region script operations
    @Override
    public Mono<Boolean> putScript(final Script script) {

        Assert.notNull(script, "script must not be null");

        return observations().mono(OpenSearchOperationName.PUT_SCRIPT, null, () -> {
            final var request = requestConverter.scriptPut(script);
            return Mono.from(execute(client -> client.putScript(request))).map(PutScriptResponse::acknowledged);
        });
    }

    @Override
    public Mono<Script> getScript(final String name) {

        Assert.notNull(name, "name must not be null");

        return observations().mono(OpenSearchOperationName.GET_SCRIPT, null, () -> {
            final var request = requestConverter.scriptGet(name);
            return Mono.from(execute(client -> client.getScript(request))).mapNotNull(responseConverter::scriptResponse);
        });
    }

    @Override
    public Mono<Boolean> deleteScript(final String name) {
        Assert.notNull(name, "name must not be null");

        return observations().mono(OpenSearchOperationName.DELETE_SCRIPT, null, () -> {
            final var request = requestConverter.scriptDelete(name);
            return Mono.from(execute(client -> client.deleteScript(request))).map(DeleteScriptResponse::acknowledged);
        });
    }
    // endregion

    @Override
    public Mono<String> getVendor() {
        return Mono.just("Elasticsearch");
    }

    @Override
    public Mono<String> getRuntimeLibraryVersion() {
        return Mono.just(Version.VERSION != null ? Version.VERSION.toString() : "null");
    }

    @Override
    public Mono<String> getClusterVersion() {
        return observations().mono(OpenSearchOperationName.CLUSTER_INFO, null,
                () -> Mono.from(execute((ReactiveOpenSearchClient reactiveElasticsearchClient) -> {
                    try (final var ignored = DANGEROUS_disableRequiredPropertiesCheck(true)) {
                        return reactiveElasticsearchClient.info();
                    }
                })).map(infoResponse -> infoResponse.version().number()));
    }

    @Override
    public ReactiveIndexOperations indexOps(final IndexCoordinates index) {
        final var template = new ReactiveIndicesTemplate(client.indices(), getReactiveClusterTemplate(), converter, index);
        template.setObservationSupportFactory(this::observations);
        return template;
    }

    @Override
    public ReactiveIndexOperations indexOps(final Class<?> clazz) {
        final var template = new ReactiveIndicesTemplate(client.indices(), getReactiveClusterTemplate(), converter, clazz);
        template.setObservationSupportFactory(this::observations);
        return template;
    }

    @Override
    public ReactiveClusterOperations cluster() {
        return getReactiveClusterTemplate();
    }

    /**
     * @since 5.1
     */
    private ReactiveClusterTemplate getReactiveClusterTemplate() {
        final var template = new ReactiveClusterTemplate(client.cluster(), converter);
        template.setObservationSupportFactory(this::observations);
        return template;
    }

    @Override
    public Query matchAllQuery() {
        return NativeQuery.builder().withQuery(Queries.matchAllQueryAsQuery()).build();
    }

    @Override
    public Query idsQuery(List<String> ids) {
        return NativeQuery.builder().withQuery(qb -> qb.ids(iq -> iq.values(ids))).build();
    }

    @Override
    public BaseQueryBuilder queryBuilderWithIds(List<String> ids) {
        return NativeQuery.builder().withIds(ids);
    }

    /**
     * Callback interface to be used with {@link #execute(ReactiveElasticsearchTemplate.ClientCallback)} for operating
     * directly on {@link ReactiveOpenSearchClient}.
     *
     * @param <T>
     */
    public interface ClientCallback<T extends Publisher<?>> {

        T doWithClient(ReactiveOpenSearchClient client);
    }

    /**
     * Execute a callback with the {@link ReactiveOpenSearchClient} and provide exception translation.
     *
     * @param callback the callback to execute, must not be {@literal null}
     * @param <T> the type returned from the callback
     * @return the callback result
     */
    public <T> Publisher<T> execute(ReactiveOpenSearchTemplate.ClientCallback<Publisher<T>> callback) {
        return Flux.defer(() -> callback.doWithClient(client)).onErrorMap(this::translateException);
    }

    /**
     * translates an Exception if possible. Exceptions that are no {@link RuntimeException}s are wrapped in a
     * RuntimeException
     *
     * @param throwable the Exception to map
     * @return the potentially translated RuntimeException.
     */
    private RuntimeException translateException(Throwable throwable) {

        RuntimeException runtimeException = throwable instanceof RuntimeException ex ? ex
                : new RuntimeException(throwable.getMessage(), throwable);
        RuntimeException potentiallyTranslatedException = exceptionTranslator
                .translateExceptionIfPossible(runtimeException);

        return potentiallyTranslatedException != null ? potentiallyTranslatedException : runtimeException;
    }

    @Override
    public Mono<SqlResponse> search(SqlQuery query) {
        throw new UnsupportedOperationException("The operation search(SqlQuery query) is not supported");
    }
}
