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
import org.opensearch.client.opensearch.cluster.DeleteComponentTemplateRequest;
import org.opensearch.client.opensearch.cluster.DeleteComponentTemplateResponse;
import org.opensearch.client.opensearch.cluster.ExistsComponentTemplateRequest;
import org.opensearch.client.opensearch.cluster.GetComponentTemplateRequest;
import org.opensearch.client.opensearch.cluster.GetComponentTemplateResponse;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.cluster.PutComponentTemplateRequest;
import org.opensearch.client.opensearch.cluster.PutComponentTemplateResponse;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.client.util.ObjectBuilder;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

/**
 * Reactive version of the {@link org.opensearch.client.opensearch.cluster.OpenSearchClusterClient}
 */
public class ReactiveOpenSearchClusterClient
        extends ApiClient<OpenSearchTransport, ReactiveOpenSearchClusterClient> {

    public ReactiveOpenSearchClusterClient(OpenSearchTransport transport,
            @Nullable TransportOptions transportOptions) {
        super(transport, transportOptions);
    }

    @Override
    public ReactiveOpenSearchClusterClient withTransportOptions(@Nullable TransportOptions transportOptions) {
        return new ReactiveOpenSearchClusterClient(transport, transportOptions);
    }

    public Mono<HealthResponse> health(HealthRequest healthRequest) {
        return Mono.fromFuture(transport.performRequestAsync(healthRequest, HealthRequest._ENDPOINT, transportOptions));
    }

    public Mono<HealthResponse> health(Function<HealthRequest.Builder, ObjectBuilder<HealthRequest>> fn) {
        return health(fn.apply(new HealthRequest.Builder()).build());
    }

    public Mono<PutComponentTemplateResponse> putComponentTemplate(
            PutComponentTemplateRequest putComponentTemplateRequest) {
        return Mono.fromFuture(transport.performRequestAsync(putComponentTemplateRequest,
                PutComponentTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<PutComponentTemplateResponse> putComponentTemplate(
            Function<PutComponentTemplateRequest.Builder, ObjectBuilder<PutComponentTemplateRequest>> fn) {
        return putComponentTemplate(fn.apply(new PutComponentTemplateRequest.Builder()).build());
    }

    public Mono<GetComponentTemplateResponse> getComponentTemplate(
            GetComponentTemplateRequest getComponentTemplateRequest) {
        return Mono.fromFuture(transport.performRequestAsync(getComponentTemplateRequest,
                GetComponentTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<GetComponentTemplateResponse> getComponentTemplate(
            Function<GetComponentTemplateRequest.Builder, ObjectBuilder<GetComponentTemplateRequest>> fn) {
        return getComponentTemplate(fn.apply(new GetComponentTemplateRequest.Builder()).build());
    }

    public Mono<BooleanResponse> existsComponentTemplate(ExistsComponentTemplateRequest existsComponentTemplateRequest) {
        return Mono.fromFuture(transport.performRequestAsync(existsComponentTemplateRequest,
                ExistsComponentTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<BooleanResponse> existsComponentTemplate(
            Function<ExistsComponentTemplateRequest.Builder, ObjectBuilder<ExistsComponentTemplateRequest>> fn) {
        return existsComponentTemplate(fn.apply(new ExistsComponentTemplateRequest.Builder()).build());
    }

    public Mono<DeleteComponentTemplateResponse> deleteComponentTemplate(
            DeleteComponentTemplateRequest deleteComponentTemplateRequest) {
        return Mono.fromFuture(transport.performRequestAsync(deleteComponentTemplateRequest,
                DeleteComponentTemplateRequest._ENDPOINT, transportOptions));
    }

    public Mono<DeleteComponentTemplateResponse> deleteComponentTemplate(
            Function<DeleteComponentTemplateRequest.Builder, ObjectBuilder<DeleteComponentTemplateRequest>> fn) {
        return deleteComponentTemplate(fn.apply(new DeleteComponentTemplateRequest.Builder()).build());
    }
}
