/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.spring.boot.autoconfigure.data;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.ExistsRequest;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.data.client.osc.DefaultOpenSearchObservationConvention;
import org.opensearch.data.client.osc.OpenSearchObservationContext;
import org.opensearch.data.client.osc.OpenSearchObservationConvention;
import org.opensearch.data.client.osc.OpenSearchTemplate;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.opensearch.data.core.OpenSearchMappingParametersCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.index.MappingParametersCustomizer;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import reactor.core.publisher.Mono;

class OpenSearchObservationAutoConfigurationTests {
    @Test
    void starterTemplatesDiscoverObservationBeansAndConfigureChildren() throws Exception {
        final var client = mock(OpenSearchClient.class, RETURNS_DEEP_STUBS);
        final var reactiveClient = mock(ReactiveOpenSearchClient.class, RETURNS_DEEP_STUBS);
        when(client._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(client.indices()._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(client.cluster()._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(reactiveClient._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(reactiveClient.indices()._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(reactiveClient.cluster()._transport().jsonpMapper()).thenReturn(new JacksonJsonpMapper());
        when(client.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        when(client.indices().exists(any(org.opensearch.client.opensearch.indices.ExistsRequest.class)))
                .thenReturn(new BooleanResponse(true));
        when(reactiveClient.exists(any(ExistsRequest.class))).thenReturn(Mono.just(new BooleanResponse(true)));
        when(reactiveClient.indices().exists(any(org.opensearch.client.opensearch.indices.ExistsRequest.class)))
                .thenReturn(Mono.just(new BooleanResponse(true)));
        final var stopped = new ArrayList<OpenSearchObservationContext>();
        final var registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(new ObservationHandler<OpenSearchObservationContext>() {
            @Override
            public boolean supportsContext(final Observation.Context context) {
                return context instanceof OpenSearchObservationContext;
            }

            @Override
            public void onStop(final OpenSearchObservationContext context) {
                stopped.add(context);
            }
        });
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OpenSearchDataAutoConfiguration.class))
                .withBean(OpenSearchClient.class, () -> client)
                .withBean(ReactiveOpenSearchClient.class, () -> reactiveClient)
                .withBean(MappingParametersCustomizer.class, OpenSearchMappingParametersCustomizer::new)
                .withBean(ObservationRegistry.class, () -> registry)
                .withBean(OpenSearchObservationConvention.class, () -> new DefaultOpenSearchObservationConvention() {
                    @Override
                    public String getName() {
                        return "custom.opensearch";
                    }
                })
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final var index = IndexCoordinates.of("observed");
                    final var template = context.getBean(OpenSearchTemplate.class);
                    final var reactive = context.getBean(ReactiveOpenSearchTemplate.class);
                    assertThat(template.withRefreshPolicy(RefreshPolicy.IMMEDIATE).exists("1", index)).isTrue();
                    assertThat(template.indexOps(index).exists()).isTrue();
                    assertThat(reactive.withRefreshPolicy(RefreshPolicy.IMMEDIATE).exists("1", index).block()).isTrue();
                    assertThat(reactive.indexOps(index).exists().block()).isTrue();
                    assertThat(stopped).hasSize(4)
                            .allSatisfy(observation -> assertThat(observation.getName()).isEqualTo("custom.opensearch"));
                });
    }
}
