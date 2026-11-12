/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._helpers.bulk.BulkListener;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.springframework.dao.DataAccessException;
import org.springframework.data.elasticsearch.core.AbstractBulkIngester;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterListener;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterOptions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Adapts the OpenSearch Java client's bulk ingester to Spring Data's mapping, callbacks and exception translation.
 *
 * @param <C> the type of the context associated with each operation
 * @since 3.2
 */
class OpenSearchBulkIngester<C> extends AbstractBulkIngester<C> {

    private final RequestConverter requestConverter;
    private final OpenSearchExceptionTranslator exceptionTranslator;
    private final org.opensearch.client.opensearch._helpers.bulk.BulkIngester<OperationContext<C>> delegate;

    OpenSearchBulkIngester(final OpenSearchTemplate template, final OpenSearchClient client,
            final RequestConverter requestConverter, final OpenSearchExceptionTranslator exceptionTranslator,
            final BulkIngesterOptions options, final IndexCoordinates index, final BulkIngesterListener<C> listener) {

        super(template, index, listener);

        Assert.notNull(client, "client must not be null");
        Assert.notNull(requestConverter, "requestConverter must not be null");
        Assert.notNull(exceptionTranslator, "exceptionTranslator must not be null");
        Assert.notNull(options, "options must not be null");

        this.requestConverter = requestConverter;
        this.exceptionTranslator = exceptionTranslator;

        final var builder =
                new org.opensearch.client.opensearch._helpers.bulk.BulkIngester.Builder<OperationContext<C>>()
                        .client(client)
                        .maxOperations(options.getMaxOperations())
                        .maxSize(options.getMaxSize())
                        .maxConcurrentRequests(options.getMaxConcurrentRequests())
                        .globalSettings(requestConverter.bulkRequestBuilder(options.getBulkOptions(), template.getRefreshPolicy()))
                        .listener(new Listener());

        final var flushInterval = options.getFlushInterval();
        if (flushInterval != null) {
            builder.flushInterval(flushInterval.toMillis(), TimeUnit.MILLISECONDS);
        }

        delegate = builder.build();
    }

    @Override
    protected void doAdd(final IndexQuery query, final OperationContext<C> context) {
        delegate.add(requestConverter.bulkOperation(query, getIndex()), context);
    }

    @Override
    protected void doAdd(final UpdateQuery query, final OperationContext<C> context) {
        delegate.add(requestConverter.bulkOperation(query, getIndex()), context);
    }

    @Override
    protected void doAddDelete(final String id, @Nullable final String routing, final OperationContext<C> context) {
        delegate.add(requestConverter.bulkDeleteOperation(id, routing, getIndex()), context);
    }

    @Override
    protected void doFlush() {
        delegate.flush();
    }

    @Override
    protected void doClose() {
        delegate.close();
    }

    @Override
    protected RuntimeException translateException(final Throwable throwable) {
        try {
            return exceptionTranslator.translateException(throwable);
        } catch (final DataAccessException translated) {
            // The translator throws some conflict exceptions instead of returning them.
            return translated;
        }
    }

    @Override
    public int getPendingOperations() {
        return delegate.pendingOperations();
    }

    @Override
    public long getPendingOperationsSize() {
        return delegate.pendingOperationsSize();
    }

    @Override
    public int getPendingRequests() {
        return delegate.pendingRequests();
    }

    @Override
    public long getOperationsCount() {
        return delegate.operationsCount();
    }

    @Override
    public long getOperationContentionsCount() {
        return delegate.operationContentionsCount();
    }

    @Override
    public long getRequestCount() {
        return delegate.requestCount();
    }

    @Override
    public long getRequestContentionsCount() {
        return delegate.requestContentionsCount();
    }

    private static ItemResponse toItemResponse(final BulkResponseItem item) {
        final var error = item.error();
        final var errorReason = error != null ? (error.reason() != null ? error.reason() : error.type()) : null;
        return new ItemResponse(item.id(), item.index(), item.status(), item.seqNo(), item.primaryTerm(), item.version(),
                errorReason);
    }

    private class Listener implements BulkListener<OperationContext<C>> {

        @Override
        public void beforeBulk(final long executionId, final BulkRequest request,
                final List<OperationContext<C>> contexts) {
            onBeforeBulk(executionId, contexts);
        }

        @Override
        public void afterBulk(final long executionId, final BulkRequest request,
                final List<OperationContext<C>> contexts, final BulkResponse response) {
            onResponse(executionId, contexts,
                    response.items().stream().map(OpenSearchBulkIngester::toItemResponse).toList());
        }

        @Override
        public void afterBulk(final long executionId, final BulkRequest request,
                final List<OperationContext<C>> contexts, final Throwable failure) {
            onFailure(executionId, contexts, failure);
        }
    }
}
