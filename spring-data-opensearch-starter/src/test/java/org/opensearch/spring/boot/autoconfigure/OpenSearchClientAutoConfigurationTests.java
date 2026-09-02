/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.opensearch.data.core.OpenSearchMappingParametersCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.index.MappingParametersCustomizer;

class OpenSearchClientAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    OpenSearchRestClientAutoConfiguration.class,
                    OpenSearchClientAutoConfiguration.class));

    @Test
    void configuresDefaultMappingParametersCustomizer() {
        this.contextRunner.run((context) -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(MappingParametersCustomizer.class)
                .hasSingleBean(OpenSearchMappingParametersCustomizer.class));
    }

    @Test
    void backsOffWhenMappingParametersCustomizerBeanUsesInterfaceReturnType() {
        this.contextRunner
                .withUserConfiguration(CustomMappingParametersCustomizerConfiguration.class)
                .run((context) -> {
                    assertThat(context).hasNotFailed().hasSingleBean(MappingParametersCustomizer.class);
                    assertThat(context.getBean(MappingParametersCustomizer.class))
                            .isSameAs(CustomMappingParametersCustomizerConfiguration.CUSTOMIZER);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomMappingParametersCustomizerConfiguration {

        private static final MappingParametersCustomizer CUSTOMIZER = mock(MappingParametersCustomizer.class);

        @Bean
        MappingParametersCustomizer opensearchMappingParametersCustomizer() {
            return CUSTOMIZER;
        }
    }
}
