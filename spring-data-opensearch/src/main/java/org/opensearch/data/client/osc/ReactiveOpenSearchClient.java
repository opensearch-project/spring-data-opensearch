/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.osc;

import java.io.IOException;
import java.util.function.Function;
import org.opensearch.client.ApiClient;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.ClearScrollRequest;
import org.opensearch.client.opensearch.core.ClearScrollResponse;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.CountResponse;
import org.opensearch.client.opensearch.core.CreatePitRequest;
import org.opensearch.client.opensearch.core.CreatePitResponse;
import org.opensearch.client.opensearch.core.DeleteByQueryRequest;
import org.opensearch.client.opensearch.core.DeleteByQueryResponse;
import org.opensearch.client.opensearch.core.DeletePitRequest;
import org.opensearch.client.opensearch.core.DeletePitResponse;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.DeleteResponse;
import org.opensearch.client.opensearch.core.DeleteScriptRequest;
import org.opensearch.client.opensearch.core.DeleteScriptResponse;
import org.opensearch.client.opensearch.core.ExistsRequest;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.GetScriptRequest;
import org.opensearch.client.opensearch.core.GetScriptResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.InfoRequest;
import org.opensearch.client.opensearch.core.InfoResponse;
import org.opensearch.client.opensearch.core.MgetRequest;
import org.opensearch.client.opensearch.core.MgetResponse;
import org.opensearch.client.opensearch.core.PingRequest;
import org.opensearch.client.opensearch.core.PutScriptRequest;
import org.opensearch.client.opensearch.core.PutScriptResponse;
import org.opensearch.client.opensearch.core.ReindexRequest;
import org.opensearch.client.opensearch.core.ReindexResponse;
import org.opensearch.client.opensearch.core.ScrollRequest;
import org.opensearch.client.opensearch.core.ScrollResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.SearchTemplateRequest;
import org.opensearch.client.opensearch.core.SearchTemplateResponse;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.opensearch.client.opensearch.core.UpdateResponse;
import org.opensearch.client.transport.JsonEndpoint;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.client.transport.endpoints.EndpointWithResponseMapperAttr;
import org.opensearch.client.util.ObjectBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * Reactive version of {@link org.opensearch.client.opensearch.OpenSearchClient}.
 */
public class ReactiveOpenSearchClient extends ApiClient<OpenSearchTransport, ReactiveOpenSearchClient>
        implements AutoCloseable {

    public ReactiveOpenSearchClient(OpenSearchTransport transport) {
        super(transport, null);
    }

    public ReactiveOpenSearchClient(OpenSearchTransport transport, @Nullable TransportOptions transportOptions) {
        super(transport, transportOptions);
    }

    @Override
    public ReactiveOpenSearchClient withTransportOptions(@Nullable TransportOptions transportOptions) {
        return new ReactiveOpenSearchClient(transport, transportOptions);
    }

    @Override
    public void close() throws IOException {
    }

    // region child clients

    public ReactiveOpenSearchClusterClient cluster() {
        return new ReactiveOpenSearchClusterClient(transport, transportOptions);
    }

    public ReactiveOpenSearchIndicesClient indices() {
        return new ReactiveOpenSearchIndicesClient(transport, transportOptions);
    }

    // endregion
    // region info

    public Mono<InfoResponse> info() {
        return Mono
                .fromFuture(transport.performRequestAsync(InfoRequest.builder().build(), InfoRequest._ENDPOINT, transportOptions));
    }

    public Mono<BooleanResponse> ping() {
        return Mono
                .fromFuture(transport.performRequestAsync(PingRequest.builder().build(), PingRequest._ENDPOINT, transportOptions));
    }

    // endregion
    // region document

    public <T> Mono<IndexResponse> index(IndexRequest<T> request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, IndexRequest._ENDPOINT, transportOptions));
    }

    public <T> Mono<IndexResponse> index(Function<IndexRequest.Builder<T>, ObjectBuilder<IndexRequest<T>>> fn) {

        Assert.notNull(fn, "fn must not be null");

        return index(fn.apply(new IndexRequest.Builder<>()).build());
    }

    public Mono<BulkResponse> bulk(BulkRequest request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, BulkRequest._ENDPOINT, transportOptions));
    }

    public Mono<BulkResponse> bulk(Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>> fn) {

        Assert.notNull(fn, "fn must not be null");

        return bulk(fn.apply(new BulkRequest.Builder()).build());
    }

    public <T> Mono<GetResponse<T>> get(GetRequest request, Class<T> tClass) {

        Assert.notNull(request, "request must not be null");

        // code adapted from
        // org.opensearch.client.opensearch.OpenSearchClient.get(org.opensearch.client.opensearch.core.GetRequest,
        // java.lang.Class<TDocument>)
        // noinspection unchecked
        JsonEndpoint<GetRequest, GetResponse<T>, ErrorResponse> endpoint = (JsonEndpoint<GetRequest, GetResponse<T>, ErrorResponse>) GetRequest._ENDPOINT;
        endpoint = new EndpointWithResponseMapperAttr<>(endpoint,
                "org.opensearch.client:Deserializer:_global.get.TDocument",
                getDeserializer(tClass));

        return Mono.fromFuture(transport.performRequestAsync(request, endpoint, transportOptions));
    }

    public Mono<BooleanResponse> exists(ExistsRequest request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, ExistsRequest._ENDPOINT, transportOptions));
    }

    public <T, P> Mono<UpdateResponse<T>> update(UpdateRequest<T, P> request, Class<T> clazz) {

        Assert.notNull(request, "request must not be null");

        // noinspection unchecked
        JsonEndpoint<UpdateRequest<?, ?>, UpdateResponse<T>, ErrorResponse> endpoint = new EndpointWithResponseMapperAttr(
                UpdateRequest._ENDPOINT, "org.opensearch.client:Deserializer:_global.update.TDocument",
                this.getDeserializer(clazz));
        return Mono.fromFuture(transport.performRequestAsync(request, endpoint, this.transportOptions));
    }

    public <T, P> Mono<UpdateResponse<T>> update(
            Function<UpdateRequest.Builder<T, P>, ObjectBuilder<UpdateRequest<T, P>>> fn, Class<T> clazz) {

        Assert.notNull(fn, "fn must not be null");

        return update(fn.apply(new UpdateRequest.Builder<>()).build(), clazz);
    }

    public <T> Mono<GetResponse<T>> get(Function<GetRequest.Builder, ObjectBuilder<GetRequest>> fn, Class<T> tClass) {
        Assert.notNull(fn, "fn must not be null");

        return get(fn.apply(new GetRequest.Builder()).build(), tClass);
    }

    public <T> Mono<MgetResponse<T>> mget(MgetRequest request, Class<T> clazz) {

        Assert.notNull(request, "request must not be null");
        Assert.notNull(clazz, "clazz must not be null");

        // noinspection unchecked
        JsonEndpoint<MgetRequest, MgetResponse<T>, ErrorResponse> endpoint = (JsonEndpoint<MgetRequest, MgetResponse<T>, ErrorResponse>) MgetRequest._ENDPOINT;
        endpoint = new EndpointWithResponseMapperAttr<>(endpoint,
                "org.opensearch.client:Deserializer:_global.mget.TDocument",
                this.getDeserializer(clazz));

        return Mono.fromFuture(transport.performRequestAsync(request, endpoint, transportOptions));
    }

    public <T> Mono<MgetResponse<T>> mget(Function<MgetRequest.Builder, ObjectBuilder<MgetRequest>> fn, Class<T> clazz) {

        Assert.notNull(fn, "fn must not be null");

        return mget(fn.apply(new MgetRequest.Builder()).build(), clazz);
    }

    public Mono<ReindexResponse> reindex(ReindexRequest request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, ReindexRequest._ENDPOINT, transportOptions));
    }

    public Mono<ReindexResponse> reindex(Function<ReindexRequest.Builder, ObjectBuilder<ReindexRequest>> fn) {

        Assert.notNull(fn, "fn must not be null");

        return reindex(fn.apply(new ReindexRequest.Builder()).build());
    }

    public Mono<DeleteResponse> delete(DeleteRequest request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, DeleteRequest._ENDPOINT, transportOptions));
    }

    public Mono<DeleteResponse> delete(Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>> fn) {

        Assert.notNull(fn, "fn must not be null");

        return delete(fn.apply(new DeleteRequest.Builder()).build());
    }

    public Mono<DeleteByQueryResponse> deleteByQuery(DeleteByQueryRequest request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, DeleteByQueryRequest._ENDPOINT, transportOptions));
    }

    public Mono<DeleteByQueryResponse> deleteByQuery(
            Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>> fn) {

        Assert.notNull(fn, "fn must not be null");

        return deleteByQuery(fn.apply(new DeleteByQueryRequest.Builder()).build());
    }

    /**
     * @since 5.4
     */
    public Mono<CountResponse> count(CountRequest request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, CountRequest._ENDPOINT, transportOptions));
    }

    /**
     * @since 5.4
     */
    public Mono<CountResponse> count(Function<CountRequest.Builder, ObjectBuilder<CountRequest>> fn) {

        Assert.notNull(fn, "fn must not be null");

        return count(fn.apply(new CountRequest.Builder()).build());
    }

    // endregion
    // region search

    public <T> Mono<SearchResponse<T>> search(SearchRequest request, Class<T> tDocumentClass) {

        Assert.notNull(request, "request must not be null");
        Assert.notNull(tDocumentClass, "tDocumentClass must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request,
                SearchRequest.createSearchEndpoint(this.getDeserializer(tDocumentClass)), transportOptions));
    }

    public <T> Mono<SearchResponse<T>> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn,
            Class<T> tDocumentClass) {

        Assert.notNull(fn, "fn must not be null");
        Assert.notNull(tDocumentClass, "tDocumentClass must not be null");

        return search(fn.apply(new SearchRequest.Builder()).build(), tDocumentClass);
    }

    /**
     * @since 5.1
     */
    public <T> Mono<SearchTemplateResponse<T>> searchTemplate(SearchTemplateRequest request, Class<T> tDocumentClass) {

        Assert.notNull(request, "request must not be null");
        Assert.notNull(tDocumentClass, "tDocumentClass must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request,
                SearchTemplateRequest.createSearchTemplateEndpoint(this.getDeserializer(tDocumentClass)), transportOptions));
    }

    /**
     * @since 5.1
     */
    public <T> Mono<SearchTemplateResponse<T>> searchTemplate(
            Function<SearchTemplateRequest.Builder, ObjectBuilder<SearchTemplateRequest>> fn, Class<T> tDocumentClass) {

        Assert.notNull(fn, "fn must not be null");

        return searchTemplate(fn.apply(new SearchTemplateRequest.Builder()).build(), tDocumentClass);
    }

    public <T> Mono<ScrollResponse<T>> scroll(ScrollRequest request, Class<T> tDocumentClass) {

        Assert.notNull(request, "request must not be null");
        Assert.notNull(tDocumentClass, "tDocumentClass must not be null");

        // code adapted from
        // org.opensearch.client.opensearch.OpenSearchClient.scroll(org.opensearch.client.opensearch.core.ScrollRequest,
        // java.lang.Class<TDocument>)
        // noinspection unchecked
        JsonEndpoint<ScrollRequest, ScrollResponse<T>, ErrorResponse> endpoint = (JsonEndpoint<ScrollRequest, ScrollResponse<T>, ErrorResponse>) ScrollRequest._ENDPOINT;
        endpoint = new EndpointWithResponseMapperAttr<>(endpoint,
                "org.opensearch.client:Deserializer:_global.scroll.TDocument", getDeserializer(tDocumentClass));

        return Mono.fromFuture(transport.performRequestAsync(request, endpoint, transportOptions));
    }

    public <T> Mono<ScrollResponse<T>> scroll(Function<ScrollRequest.Builder, ObjectBuilder<ScrollRequest>> fn,
            Class<T> tDocumentClass) {

        Assert.notNull(fn, "fn must not be null");
        Assert.notNull(tDocumentClass, "tDocumentClass must not be null");

        return scroll(fn.apply(new ScrollRequest.Builder()).build(), tDocumentClass);
    }

    public Mono<ClearScrollResponse> clearScroll(ClearScrollRequest request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, ClearScrollRequest._ENDPOINT, transportOptions));
    }

    public Mono<ClearScrollResponse> clearScroll(
            Function<ClearScrollRequest.Builder, ObjectBuilder<ClearScrollRequest>> fn) {

        Assert.notNull(fn, "fn must not be null");

        return clearScroll(fn.apply(new ClearScrollRequest.Builder()).build());
    }

    // endregion

    // region script api
    /**
     * @since 5.1
     */
    public Mono<PutScriptResponse> putScript(PutScriptRequest request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, PutScriptRequest._ENDPOINT, transportOptions));
    }

    /**
     * @since 5.1
     */
    public Mono<PutScriptResponse> putScript(Function<PutScriptRequest.Builder, ObjectBuilder<PutScriptRequest>> fn) {

        Assert.notNull(fn, "fn must not be null");

        return putScript(fn.apply(new PutScriptRequest.Builder()).build());
    }

    /**
     * @since 5.1
     */
    public Mono<GetScriptResponse> getScript(GetScriptRequest request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, GetScriptRequest._ENDPOINT, transportOptions));
    }

    /**
     * @since 5.1
     */
    public Mono<GetScriptResponse> getScript(Function<GetScriptRequest.Builder, ObjectBuilder<GetScriptRequest>> fn) {

        Assert.notNull(fn, "fn must not be null");

        return getScript(fn.apply(new GetScriptRequest.Builder()).build());
    }

    /**
     * @since 5.1
     */
    public Mono<DeleteScriptResponse> deleteScript(DeleteScriptRequest request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, DeleteScriptRequest._ENDPOINT, transportOptions));
    }

    /**
     * @since 5.1
     */
    public Mono<DeleteScriptResponse> deleteScript(
            Function<DeleteScriptRequest.Builder, ObjectBuilder<DeleteScriptRequest>> fn) {

        Assert.notNull(fn, "fn must not be null");

        return deleteScript(fn.apply(new DeleteScriptRequest.Builder()).build());
    }

       /**
     * @since 5.0
     */
    public Mono<CreatePitResponse> openPointInTime(CreatePitRequest request) {

        Assert.notNull(request, "request must not be null");

        return Mono.fromFuture(transport.performRequestAsync(request, CreatePitRequest._ENDPOINT, transportOptions));
    }

   /*
    * @since 5.0
    */
   public Mono<DeletePitResponse> closePointInTime(DeletePitRequest request) {

       Assert.notNull(request, "request must not be null");

       return Mono.fromFuture(transport.performRequestAsync(request, DeletePitRequest._ENDPOINT, transportOptions));
   }

   /**
    * @since 5.0
    */
   public Mono<DeletePitResponse> closePointInTime(
           Function<DeletePitRequest.Builder, ObjectBuilder<DeletePitRequest>> fn) {

       Assert.notNull(fn, "fn must not be null");

       return closePointInTime(fn.apply(new DeletePitRequest.Builder()).build());
   }

    // endregion
}
