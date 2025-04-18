/*
 * Copyright 2021-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 *
 * @author Peter-Josef Meisch
 * @since 4.4
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
