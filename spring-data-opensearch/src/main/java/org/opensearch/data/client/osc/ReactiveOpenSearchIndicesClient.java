/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.osc;

import java.util.function.Function;
import org.opensearch.client.ApiClient;
import org.opensearch.client.opensearch.indices.AddBlockRequest;
import org.opensearch.client.opensearch.indices.AddBlockResponse;
import org.opensearch.client.opensearch.indices.AnalyzeRequest;
import org.opensearch.client.opensearch.indices.AnalyzeResponse;
import org.opensearch.client.opensearch.indices.ClearCacheRequest;
import org.opensearch.client.opensearch.indices.ClearCacheResponse;
import org.opensearch.client.opensearch.indices.CloneIndexRequest;
import org.opensearch.client.opensearch.indices.CloneIndexResponse;
import org.opensearch.client.opensearch.indices.CloseIndexRequest;
import org.opensearch.client.opensearch.indices.CloseIndexResponse;
import org.opensearch.client.opensearch.indices.CreateDataStreamRequest;
import org.opensearch.client.opensearch.indices.CreateDataStreamResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DataStreamsStatsRequest;
import org.opensearch.client.opensearch.indices.DataStreamsStatsResponse;
import org.opensearch.client.opensearch.indices.DeleteAliasRequest;
import org.opensearch.client.opensearch.indices.DeleteAliasResponse;
import org.opensearch.client.opensearch.indices.DeleteDataStreamRequest;
import org.opensearch.client.opensearch.indices.DeleteDataStreamResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexTemplateResponse;
import org.opensearch.client.opensearch.indices.DeleteTemplateRequest;
import org.opensearch.client.opensearch.indices.DeleteTemplateResponse;
import org.opensearch.client.opensearch.indices.DiskUsageRequest;
import org.opensearch.client.opensearch.indices.DiskUsageResponse;
import org.opensearch.client.opensearch.indices.ExistsAliasRequest;
import org.opensearch.client.opensearch.indices.ExistsIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.ExistsTemplateRequest;
import org.opensearch.client.opensearch.indices.FlushRequest;
import org.opensearch.client.opensearch.indices.FlushResponse;
import org.opensearch.client.opensearch.indices.ForcemergeRequest;
import org.opensearch.client.opensearch.indices.ForcemergeResponse;
import org.opensearch.client.opensearch.indices.GetAliasRequest;
import org.opensearch.client.opensearch.indices.GetAliasResponse;
import org.opensearch.client.opensearch.indices.GetDataStreamRequest;
import org.opensearch.client.opensearch.indices.GetDataStreamResponse;
import org.opensearch.client.opensearch.indices.GetFieldMappingRequest;
import org.opensearch.client.opensearch.indices.GetFieldMappingResponse;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndexResponse;
import org.opensearch.client.opensearch.indices.GetIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.GetIndexTemplateResponse;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.GetMappingRequest;
import org.opensearch.client.opensearch.indices.GetMappingResponse;
import org.opensearch.client.opensearch.indices.GetTemplateRequest;
import org.opensearch.client.opensearch.indices.GetTemplateResponse;
import org.opensearch.client.opensearch.indices.IndicesStatsRequest;
import org.opensearch.client.opensearch.indices.IndicesStatsResponse;
import org.opensearch.client.opensearch.indices.OpenRequest;
import org.opensearch.client.opensearch.indices.OpenResponse;
import org.opensearch.client.opensearch.indices.PutAliasRequest;
import org.opensearch.client.opensearch.indices.PutAliasResponse;
import org.opensearch.client.opensearch.indices.PutIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.PutIndexTemplateResponse;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.PutIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.PutMappingRequest;
import org.opensearch.client.opensearch.indices.PutMappingResponse;
import org.opensearch.client.opensearch.indices.PutTemplateRequest;
import org.opensearch.client.opensearch.indices.PutTemplateResponse;
import org.opensearch.client.opensearch.indices.RecoveryRequest;
import org.opensearch.client.opensearch.indices.RecoveryResponse;
import org.opensearch.client.opensearch.indices.RefreshRequest;
import org.opensearch.client.opensearch.indices.RefreshResponse;
import org.opensearch.client.opensearch.indices.ResolveIndexRequest;
import org.opensearch.client.opensearch.indices.ResolveIndexResponse;
import org.opensearch.client.opensearch.indices.RolloverRequest;
import org.opensearch.client.opensearch.indices.RolloverResponse;
import org.opensearch.client.opensearch.indices.SegmentsRequest;
import org.opensearch.client.opensearch.indices.SegmentsResponse;
import org.opensearch.client.opensearch.indices.ShardStoresRequest;
import org.opensearch.client.opensearch.indices.ShardStoresResponse;
import org.opensearch.client.opensearch.indices.ShrinkRequest;
import org.opensearch.client.opensearch.indices.ShrinkResponse;
import org.opensearch.client.opensearch.indices.SimulateIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.SimulateIndexTemplateResponse;
import org.opensearch.client.opensearch.indices.SimulateTemplateRequest;
import org.opensearch.client.opensearch.indices.SimulateTemplateResponse;
import org.opensearch.client.opensearch.indices.SplitRequest;
import org.opensearch.client.opensearch.indices.SplitResponse;
import org.opensearch.client.opensearch.indices.UpdateAliasesRequest;
import org.opensearch.client.opensearch.indices.UpdateAliasesResponse;
import org.opensearch.client.opensearch.indices.ValidateQueryRequest;
import org.opensearch.client.opensearch.indices.ValidateQueryResponse;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.client.util.ObjectBuilder;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

/**
 * Reactive version of the {@link org.opensearch.client.opensearch.indices.OpenSearchIndicesClient}
 */
public class ReactiveOpenSearchIndicesClient
        extends ApiClient<OpenSearchTransport, ReactiveOpenSearchIndicesClient> {

    public ReactiveOpenSearchIndicesClient(OpenSearchTransport transport,
            @Nullable TransportOptions transportOptions) {
        super(transport, transportOptions);
    }

    @Override
    public ReactiveOpenSearchIndicesClient withTransportOptions(@Nullable TransportOptions transportOptions) {
        return new ReactiveOpenSearchIndicesClient(transport, transportOptions);
    }

    public Mono<AddBlockResponse> addBlock(AddBlockRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, AddBlockRequest._ENDPOINT, transportOptions));
    }

    public Mono<AddBlockResponse> addBlock(Function<AddBlockRequest.Builder, ObjectBuilder<AddBlockRequest>> fn) {
        return addBlock(fn.apply(new AddBlockRequest.Builder()).build());
    }

    public Mono<AnalyzeResponse> analyze(AnalyzeRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, AnalyzeRequest._ENDPOINT, transportOptions));
    }

    public Mono<AnalyzeResponse> analyze(Function<AnalyzeRequest.Builder, ObjectBuilder<AnalyzeRequest>> fn) {
        return analyze(fn.apply(new AnalyzeRequest.Builder()).build());
    }

    public Mono<AnalyzeResponse> analyze() {
        return analyze(builder -> builder);
    }

    public Mono<ClearCacheResponse> clearCache(ClearCacheRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, ClearCacheRequest._ENDPOINT, transportOptions));
    }

    public Mono<ClearCacheResponse> clearCache(Function<ClearCacheRequest.Builder, ObjectBuilder<ClearCacheRequest>> fn) {
        return clearCache(fn.apply(new ClearCacheRequest.Builder()).build());
    }

    public Mono<ClearCacheResponse> clearCache() {
        return clearCache(builder -> builder);
    }

    public Mono<CloneIndexResponse> clone(CloneIndexRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, CloneIndexRequest._ENDPOINT, transportOptions));
    }

    public Mono<CloneIndexResponse> clone(Function<CloneIndexRequest.Builder, ObjectBuilder<CloneIndexRequest>> fn) {
        return clone(fn.apply(new CloneIndexRequest.Builder()).build());
    }

    public Mono<CloseIndexResponse> close(CloseIndexRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, CloseIndexRequest._ENDPOINT, transportOptions));
    }

    public Mono<CloseIndexResponse> close(Function<CloseIndexRequest.Builder, ObjectBuilder<CloseIndexRequest>> fn) {
        return close(fn.apply(new CloseIndexRequest.Builder()).build());
    }

    public Mono<CreateIndexResponse> create(CreateIndexRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, CreateIndexRequest._ENDPOINT, transportOptions));
    }

    public Mono<CreateIndexResponse> create(Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>> fn) {
        return create(fn.apply(new CreateIndexRequest.Builder()).build());
    }

    public Mono<CreateDataStreamResponse> createDataStream(CreateDataStreamRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, CreateDataStreamRequest._ENDPOINT, transportOptions));
    }

    public Mono<CreateDataStreamResponse> createDataStream(
            Function<CreateDataStreamRequest.Builder, ObjectBuilder<CreateDataStreamRequest>> fn) {
        return createDataStream(fn.apply(new CreateDataStreamRequest.Builder()).build());
    }

    public Mono<DataStreamsStatsResponse> dataStreamsStats(DataStreamsStatsRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, DataStreamsStatsRequest._ENDPOINT, transportOptions));
    }

    public Mono<DataStreamsStatsResponse> dataStreamsStats(
            Function<DataStreamsStatsRequest.Builder, ObjectBuilder<DataStreamsStatsRequest>> fn) {
        return dataStreamsStats(fn.apply(new DataStreamsStatsRequest.Builder()).build());
    }

    public Mono<DataStreamsStatsResponse> dataStreamsStats() {
        return dataStreamsStats(builder -> builder);
    }

    public Mono<DeleteIndexResponse> delete(DeleteIndexRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, DeleteIndexRequest._ENDPOINT, transportOptions));
    }

    public Mono<DeleteIndexResponse> delete(Function<DeleteIndexRequest.Builder, ObjectBuilder<DeleteIndexRequest>> fn) {
        return delete(fn.apply(new DeleteIndexRequest.Builder()).build());
    }

    public Mono<DeleteAliasResponse> deleteAlias(DeleteAliasRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, DeleteAliasRequest._ENDPOINT, transportOptions));
    }

    public Mono<DeleteAliasResponse> deleteAlias(
            Function<DeleteAliasRequest.Builder, ObjectBuilder<DeleteAliasRequest>> fn) {
        return deleteAlias(fn.apply(new DeleteAliasRequest.Builder()).build());
    }

    public Mono<DeleteDataStreamResponse> deleteDataStream(DeleteDataStreamRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, DeleteDataStreamRequest._ENDPOINT, transportOptions));
    }

    public Mono<DeleteDataStreamResponse> deleteDataStream(
            Function<DeleteDataStreamRequest.Builder, ObjectBuilder<DeleteDataStreamRequest>> fn) {
        return deleteDataStream(fn.apply(new DeleteDataStreamRequest.Builder()).build());
    }

    public Mono<DeleteIndexTemplateResponse> deleteIndexTemplate(DeleteIndexTemplateRequest request) {
        return Mono
                .fromFuture(transport.performRequestAsync(request, DeleteIndexTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<DeleteIndexTemplateResponse> deleteIndexTemplate(
            Function<DeleteIndexTemplateRequest.Builder, ObjectBuilder<DeleteIndexTemplateRequest>> fn) {
        return deleteIndexTemplate(fn.apply(new DeleteIndexTemplateRequest.Builder()).build());
    }

    public Mono<DeleteTemplateResponse> deleteTemplate(DeleteTemplateRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, DeleteTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<DeleteTemplateResponse> deleteTemplate(
            Function<DeleteTemplateRequest.Builder, ObjectBuilder<DeleteTemplateRequest>> fn) {
        return deleteTemplate(fn.apply(new DeleteTemplateRequest.Builder()).build());
    }

    public Mono<DiskUsageResponse> diskUsage(DiskUsageRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, DiskUsageRequest._ENDPOINT, transportOptions));
    }

    public Mono<DiskUsageResponse> diskUsage(Function<DiskUsageRequest.Builder, ObjectBuilder<DiskUsageRequest>> fn) {
        return diskUsage(fn.apply(new DiskUsageRequest.Builder()).build());
    }

    public Mono<BooleanResponse> exists(ExistsRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, ExistsRequest._ENDPOINT, transportOptions));
    }

    public Mono<BooleanResponse> exists(Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>> fn) {
        return exists(fn.apply(new ExistsRequest.Builder()).build());
    }

    public Mono<BooleanResponse> existsAlias(ExistsAliasRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, ExistsAliasRequest._ENDPOINT, transportOptions));
    }

    public Mono<BooleanResponse> existsAlias(Function<ExistsAliasRequest.Builder, ObjectBuilder<ExistsAliasRequest>> fn) {
        return existsAlias(fn.apply(new ExistsAliasRequest.Builder()).build());
    }

    public Mono<BooleanResponse> existsIndexTemplate(ExistsIndexTemplateRequest request) {
        return Mono
                .fromFuture(transport.performRequestAsync(request, ExistsIndexTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<BooleanResponse> existsIndexTemplate(
            Function<ExistsIndexTemplateRequest.Builder, ObjectBuilder<ExistsIndexTemplateRequest>> fn) {
        return existsIndexTemplate(fn.apply(new ExistsIndexTemplateRequest.Builder()).build());
    }

    public Mono<BooleanResponse> existsTemplate(ExistsTemplateRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, ExistsTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<BooleanResponse> existsTemplate(
            Function<ExistsTemplateRequest.Builder, ObjectBuilder<ExistsTemplateRequest>> fn) {
        return existsTemplate(fn.apply(new ExistsTemplateRequest.Builder()).build());
    }

    public Mono<FlushResponse> flush(FlushRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, FlushRequest._ENDPOINT, transportOptions));
    }

    public Mono<FlushResponse> flush(Function<FlushRequest.Builder, ObjectBuilder<FlushRequest>> fn) {
        return flush(fn.apply(new FlushRequest.Builder()).build());
    }

    public Mono<FlushResponse> flush() {
        return flush(builder -> builder);
    }

    @SuppressWarnings("SpellCheckingInspection")
    public Mono<ForcemergeResponse> forcemerge(ForcemergeRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, ForcemergeRequest._ENDPOINT, transportOptions));
    }

    @SuppressWarnings("SpellCheckingInspection")
    public Mono<ForcemergeResponse> forcemerge(Function<ForcemergeRequest.Builder, ObjectBuilder<ForcemergeRequest>> fn) {
        return forcemerge(fn.apply(new ForcemergeRequest.Builder()).build());
    }

    @SuppressWarnings("SpellCheckingInspection")
    public Mono<ForcemergeResponse> forcemerge() {
        return forcemerge(builder -> builder);
    }

    public Mono<GetIndexResponse> get(GetIndexRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, GetIndexRequest._ENDPOINT, transportOptions));
    }

    public Mono<GetIndexResponse> get(Function<GetIndexRequest.Builder, ObjectBuilder<GetIndexRequest>> fn) {
        return get(fn.apply(new GetIndexRequest.Builder()).build());
    }

    public Mono<GetAliasResponse> getAlias(GetAliasRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, GetAliasRequest._ENDPOINT, transportOptions));
    }

    public Mono<GetAliasResponse> getAlias(Function<GetAliasRequest.Builder, ObjectBuilder<GetAliasRequest>> fn) {
        return getAlias(fn.apply(new GetAliasRequest.Builder()).build());
    }

    public Mono<GetAliasResponse> getAlias() {
        return getAlias(builder -> builder);
    }

    public Mono<GetDataStreamResponse> getDataStream(GetDataStreamRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, GetDataStreamRequest._ENDPOINT, transportOptions));
    }

    public Mono<GetDataStreamResponse> getDataStream(
            Function<GetDataStreamRequest.Builder, ObjectBuilder<GetDataStreamRequest>> fn) {
        return getDataStream(fn.apply(new GetDataStreamRequest.Builder()).build());
    }

    public Mono<GetDataStreamResponse> getDataStream() {
        return getDataStream(builder -> builder);
    }

    public Mono<GetFieldMappingResponse> getFieldMapping(GetFieldMappingRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, GetFieldMappingRequest._ENDPOINT, transportOptions));
    }

    public Mono<GetFieldMappingResponse> getFieldMapping(
            Function<GetFieldMappingRequest.Builder, ObjectBuilder<GetFieldMappingRequest>> fn) {
        return getFieldMapping(fn.apply(new GetFieldMappingRequest.Builder()).build());
    }

    public Mono<GetIndexTemplateResponse> getIndexTemplate(GetIndexTemplateRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, GetIndexTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<GetIndexTemplateResponse> getIndexTemplate(
            Function<GetIndexTemplateRequest.Builder, ObjectBuilder<GetIndexTemplateRequest>> fn) {
        return getIndexTemplate(fn.apply(new GetIndexTemplateRequest.Builder()).build());
    }

    public Mono<GetIndexTemplateResponse> getIndexTemplate() {
        return getIndexTemplate(builder -> builder);
    }

    public Mono<GetMappingResponse> getMapping(GetMappingRequest getMappingRequest) {
        return Mono
                .fromFuture(transport.performRequestAsync(getMappingRequest, GetMappingRequest._ENDPOINT, transportOptions));
    }

    public Mono<GetMappingResponse> getMapping(Function<GetMappingRequest.Builder, ObjectBuilder<GetMappingRequest>> fn) {
        return getMapping(fn.apply(new GetMappingRequest.Builder()).build());
    }

    public Mono<GetMappingResponse> getMapping() {
        return getMapping(builder -> builder);
    }

    public Mono<GetIndicesSettingsResponse> getSettings(GetIndicesSettingsRequest request) {
        return Mono
                .fromFuture(transport.performRequestAsync(request, GetIndicesSettingsRequest._ENDPOINT, transportOptions));
    }

    public Mono<GetIndicesSettingsResponse> getSettings(
            Function<GetIndicesSettingsRequest.Builder, ObjectBuilder<GetIndicesSettingsRequest>> fn) {
        return getSettings(fn.apply(new GetIndicesSettingsRequest.Builder()).build());
    }

    public Mono<GetIndicesSettingsResponse> getSettings() {
        return getSettings(builder -> builder);
    }

    public Mono<GetTemplateResponse> getTemplate(GetTemplateRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, GetTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<GetTemplateResponse> getTemplate(
            Function<GetTemplateRequest.Builder, ObjectBuilder<GetTemplateRequest>> fn) {
        return getTemplate(fn.apply(new GetTemplateRequest.Builder()).build());
    }

    public Mono<GetTemplateResponse> getTemplate() {
        return getTemplate(builder -> builder);
    }

    public Mono<OpenResponse> open(OpenRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, OpenRequest._ENDPOINT, transportOptions));
    }

    public Mono<OpenResponse> open(Function<OpenRequest.Builder, ObjectBuilder<OpenRequest>> fn) {
        return open(fn.apply(new OpenRequest.Builder()).build());
    }

    public Mono<PutAliasResponse> putAlias(PutAliasRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, PutAliasRequest._ENDPOINT, transportOptions));
    }

    public Mono<PutAliasResponse> putAlias(Function<PutAliasRequest.Builder, ObjectBuilder<PutAliasRequest>> fn) {
        return putAlias(fn.apply(new PutAliasRequest.Builder()).build());
    }

    public Mono<PutIndexTemplateResponse> putIndexTemplate(PutIndexTemplateRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, PutIndexTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<PutIndexTemplateResponse> putIndexTemplate(
            Function<PutIndexTemplateRequest.Builder, ObjectBuilder<PutIndexTemplateRequest>> fn) {
        return putIndexTemplate(fn.apply(new PutIndexTemplateRequest.Builder()).build());
    }

    public Mono<PutMappingResponse> putMapping(PutMappingRequest putMappingRequest) {
        return Mono
                .fromFuture(transport.performRequestAsync(putMappingRequest, PutMappingRequest._ENDPOINT, transportOptions));
    }

    public Mono<PutMappingResponse> putMapping(Function<PutMappingRequest.Builder, ObjectBuilder<PutMappingRequest>> fn) {
        return putMapping(fn.apply(new PutMappingRequest.Builder()).build());
    }

    public Mono<PutIndicesSettingsResponse> putSettings(PutIndicesSettingsRequest request) {
        return Mono
                .fromFuture(transport.performRequestAsync(request, PutIndicesSettingsRequest._ENDPOINT, transportOptions));
    }

    public Mono<PutIndicesSettingsResponse> putSettings(
            Function<PutIndicesSettingsRequest.Builder, ObjectBuilder<PutIndicesSettingsRequest>> fn) {
        return putSettings(fn.apply(new PutIndicesSettingsRequest.Builder()).build());
    }

    public Mono<PutIndicesSettingsResponse> putSettings() {
        return putSettings(builder -> builder);
    }

    public Mono<PutTemplateResponse> putTemplate(PutTemplateRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, PutTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<PutTemplateResponse> putTemplate(
            Function<PutTemplateRequest.Builder, ObjectBuilder<PutTemplateRequest>> fn) {
        return putTemplate(fn.apply(new PutTemplateRequest.Builder()).build());
    }

    public Mono<RecoveryResponse> recovery(RecoveryRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, RecoveryRequest._ENDPOINT, transportOptions));
    }

    public Mono<RecoveryResponse> recovery(Function<RecoveryRequest.Builder, ObjectBuilder<RecoveryRequest>> fn) {
        return recovery(fn.apply(new RecoveryRequest.Builder()).build());
    }

    public Mono<RecoveryResponse> recovery() {
        return recovery(builder -> builder);
    }

    public Mono<RefreshResponse> refresh(RefreshRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, RefreshRequest._ENDPOINT, transportOptions));
    }

    public Mono<RefreshResponse> refresh(Function<RefreshRequest.Builder, ObjectBuilder<RefreshRequest>> fn) {
        return refresh(fn.apply(new RefreshRequest.Builder()).build());
    }

    public Mono<RefreshResponse> refresh() {
        return refresh(builder -> builder);
    }

    public Mono<ResolveIndexResponse> resolveIndex(ResolveIndexRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, ResolveIndexRequest._ENDPOINT, transportOptions));
    }

    public Mono<ResolveIndexResponse> resolveIndex(
            Function<ResolveIndexRequest.Builder, ObjectBuilder<ResolveIndexRequest>> fn) {
        return resolveIndex(fn.apply(new ResolveIndexRequest.Builder()).build());
    }

    public Mono<RolloverResponse> rollover(RolloverRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, RolloverRequest._ENDPOINT, transportOptions));
    }

    public Mono<RolloverResponse> rollover(Function<RolloverRequest.Builder, ObjectBuilder<RolloverRequest>> fn) {
        return rollover(fn.apply(new RolloverRequest.Builder()).build());
    }

    public Mono<SegmentsResponse> segments(SegmentsRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, SegmentsRequest._ENDPOINT, transportOptions));
    }

    public Mono<SegmentsResponse> segments(Function<SegmentsRequest.Builder, ObjectBuilder<SegmentsRequest>> fn) {
        return segments(fn.apply(new SegmentsRequest.Builder()).build());
    }

    public Mono<SegmentsResponse> segments() {
        return segments(builder -> builder);
    }

    public Mono<ShardStoresResponse> shardStores(ShardStoresRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, ShardStoresRequest._ENDPOINT, transportOptions));
    }

    public Mono<ShardStoresResponse> shardStores(
            Function<ShardStoresRequest.Builder, ObjectBuilder<ShardStoresRequest>> fn) {
        return shardStores(fn.apply(new ShardStoresRequest.Builder()).build());
    }

    public Mono<ShardStoresResponse> shardStores() {
        return shardStores(builder -> builder);
    }

    public Mono<ShrinkResponse> shrink(ShrinkRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, ShrinkRequest._ENDPOINT, transportOptions));
    }

    public Mono<ShrinkResponse> shrink(Function<ShrinkRequest.Builder, ObjectBuilder<ShrinkRequest>> fn) {
        return shrink(fn.apply(new ShrinkRequest.Builder()).build());
    }

    public Mono<SimulateIndexTemplateResponse> simulateIndexTemplate(SimulateIndexTemplateRequest request) {
        return Mono
                .fromFuture(transport.performRequestAsync(request, SimulateIndexTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<SimulateIndexTemplateResponse> simulateIndexTemplate(
            Function<SimulateIndexTemplateRequest.Builder, ObjectBuilder<SimulateIndexTemplateRequest>> fn) {
        return simulateIndexTemplate(fn.apply(new SimulateIndexTemplateRequest.Builder()).build());
    }

    public Mono<SimulateTemplateResponse> simulateTemplate(SimulateTemplateRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, SimulateTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<SimulateTemplateResponse> simulateTemplate(
            Function<SimulateTemplateRequest.Builder, ObjectBuilder<SimulateTemplateRequest>> fn) {
        return simulateTemplate(fn.apply(new SimulateTemplateRequest.Builder()).build());
    }

    public Mono<SimulateTemplateResponse> simulateTemplate() {
        return simulateTemplate(builder -> builder);
    }

    public Mono<SplitResponse> split(SplitRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, SplitRequest._ENDPOINT, transportOptions));
    }

    public Mono<SplitResponse> split(Function<SplitRequest.Builder, ObjectBuilder<SplitRequest>> fn) {
        return split(fn.apply(new SplitRequest.Builder()).build());
    }

    public Mono<IndicesStatsResponse> stats(IndicesStatsRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, IndicesStatsRequest._ENDPOINT, transportOptions));
    }

    public Mono<IndicesStatsResponse> stats(
            Function<IndicesStatsRequest.Builder, ObjectBuilder<IndicesStatsRequest>> fn) {
        return stats(fn.apply(new IndicesStatsRequest.Builder()).build());
    }

    public Mono<IndicesStatsResponse> stats() {
        return stats(builder -> builder);
    }

    public Mono<UpdateAliasesResponse> updateAliases(UpdateAliasesRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, UpdateAliasesRequest._ENDPOINT, transportOptions));
    }

    public Mono<UpdateAliasesResponse> updateAliases(
            Function<UpdateAliasesRequest.Builder, ObjectBuilder<UpdateAliasesRequest>> fn) {
        return updateAliases(fn.apply(new UpdateAliasesRequest.Builder()).build());
    }

    public Mono<UpdateAliasesResponse> updateAliases() {
        return updateAliases(builder -> builder);
    }

    public Mono<ValidateQueryResponse> validateQuery(ValidateQueryRequest request) {
        return Mono.fromFuture(transport.performRequestAsync(request, ValidateQueryRequest._ENDPOINT, transportOptions));
    }

    public Mono<ValidateQueryResponse> validateQuery(
            Function<ValidateQueryRequest.Builder, ObjectBuilder<ValidateQueryRequest>> fn) {
        return validateQuery(fn.apply(new ValidateQueryRequest.Builder()).build());
    }

    public Mono<ValidateQueryResponse> validateQuery() {
        return validateQuery(builder -> builder);
    }

}
