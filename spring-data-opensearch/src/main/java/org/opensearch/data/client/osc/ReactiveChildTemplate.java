/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.osc;

import org.opensearch.client.ApiClient;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.transport.Transport;
import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

/**
 * base class for a reactive template that uses on of the {@link ReactiveOpenSearchClient}'s child clients.
 */
public class ReactiveChildTemplate<T extends Transport, CLIENT extends ApiClient<T, CLIENT>> {
    protected final CLIENT client;
    protected final ElasticsearchConverter elasticsearchConverter;
    protected final RequestConverter requestConverter;
    protected final ResponseConverter responseConverter;
    protected final OpenSearchExceptionTranslator exceptionTranslator;

    public ReactiveChildTemplate(CLIENT client, ElasticsearchConverter elasticsearchConverter) {
        this.client = client;
        this.elasticsearchConverter = elasticsearchConverter;
        JsonpMapper jsonpMapper = client._transport().jsonpMapper();
        requestConverter = new RequestConverter(elasticsearchConverter, jsonpMapper);
        responseConverter = new ResponseConverter(jsonpMapper);
        exceptionTranslator = new OpenSearchExceptionTranslator(jsonpMapper);
    }

    /**
     * Callback interface to be used with {@link #execute(ClientCallback)} for operating directly on the client.
     */
    @FunctionalInterface
    public interface ClientCallback<CLIENT, RESULT extends Publisher<?>> {
        RESULT doWithClient(CLIENT client);
    }

    /**
     * Execute a callback with the client and provide exception translation.
     *
     * @param callback the callback to execute, must not be {@literal null}
     * @param <RESULT> the type returned from the callback
     * @return the callback result
     */
    public <RESULT> Publisher<RESULT> execute(ClientCallback<CLIENT, Publisher<RESULT>> callback) {

        Assert.notNull(callback, "callback must not be null");

        return Flux.defer(() -> callback.doWithClient(client)).onErrorMap(exceptionTranslator::translateException);
    }

}
