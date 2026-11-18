/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.spring.boot.autoconfigure.data;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.jackson3.JacksonJsonpMapper;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.data.client.osc.DefaultOpenSearchObservationConvention;
import org.opensearch.data.client.osc.OpenSearchObservationConvention;
import org.opensearch.data.client.osc.OpenSearchTemplate;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.opensearch.spring.boot.autoconfigure.OpenSearchClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.ReactiveOpenSearchClientAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.util.ReflectionTestUtils;

class OpenSearchObservationAutoConfigurationTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader("org.opensearch.client.sniff"))
            .withConfiguration(AutoConfigurations.of(
                    ReactiveOpenSearchClientAutoConfiguration.class,
                    OpenSearchRestClientAutoConfiguration.class,
                    OpenSearchClientAutoConfiguration.class,
                    OpenSearchDataAutoConfiguration.class));

    @Test
    void openSearchTemplateDiscoverObservationBeansAndConfigureChildren() throws Exception {
        final var registry = ObservationRegistry.create();
        contextRunner
            .withUserConfiguration(TestConfiguration.class)
            .withBean(ObservationRegistry.class, () -> registry)
            .withBean(OpenSearchObservationConvention.class, () -> new DefaultOpenSearchObservationConvention() {
                @Override
                public String getName() {
                    return "custom.opensearch";
                }
            })
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(OpenSearchObservationConvention.class);
                final var index = IndexCoordinates.of("observed");
                final var template = context.getBean(OpenSearchTemplate.class);
                final Object observationSupport = ReflectionTestUtils.getField(template, "observationSupport");
                final var ops = template.indexOps(index);
                assertThat(ReflectionTestUtils.getField(ops, "observationSupport")).isEqualTo(observationSupport);
            });
    }

    @Test
    void openSearchReactiveTemplateTemplatesDiscoverObservationBeansAndConfigureChildren() throws Exception {
        final var registry = ObservationRegistry.create();
        contextRunner
             .withUserConfiguration(TestConfiguration.class)
            .withBean(ObservationRegistry.class, () -> registry)
            .withBean(OpenSearchObservationConvention.class, () -> new DefaultOpenSearchObservationConvention() {
                @Override
                public String getName() {
                    return "custom.opensearch";
                }
            })
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(OpenSearchObservationConvention.class);
                final var index = IndexCoordinates.of("observed");
                final var template = context.getBean(ReactiveOpenSearchTemplate.class);
                final Object observationSupport = ReflectionTestUtils.getField(template, "observationSupport");
                final var ops = template.indexOps(index);
                assertThat(ReflectionTestUtils.getField(ops, "observationSupport")).isEqualTo(observationSupport);
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {
        @Bean
        OpenSearchTransport openSearchTransport() {
            final OpenSearchTransport transport = mock(OpenSearchTransport.class);
            when(transport.jsonpMapper()).thenReturn(new JacksonJsonpMapper());
            return transport;
        }
    }
}
