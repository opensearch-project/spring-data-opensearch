/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.Collections;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestHighLevelClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.data.entity.Product;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Tests for {@link OpenSearchDataAutoConfiguration}.
 *
 * Adaptation of the {@link org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfigurationTests} to
 * the needs of OpenSearch.
 */
class OpenSearchDataAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader("org.opensearch.client.sniff"))
            .withConfiguration(AutoConfigurations.of(
                    OpenSearchRestClientAutoConfiguration.class,
                    OpenSearchRestHighLevelClientAutoConfiguration.class,
                    OpenSearchDataAutoConfiguration.class));

    @Test
    void defaultRestBeansRegistered() {
        this.contextRunner.run((context) -> assertThat(context)
                .hasSingleBean(OpenSearchRestTemplate.class)
                .hasSingleBean(ElasticsearchConverter.class)
                .hasSingleBean(ElasticsearchCustomConversions.class));
    }

    @Test
    void defaultConversionsRegisterBigDecimalAsSimpleType() {
        this.contextRunner.run((context) -> {
            SimpleElasticsearchMappingContext mappingContext = context.getBean(SimpleElasticsearchMappingContext.class);
            assertThat(mappingContext)
                    .extracting("simpleTypeHolder", InstanceOfAssertFactories.type(SimpleTypeHolder.class))
                    .satisfies((simpleTypeHolder) -> assertThat(simpleTypeHolder.isSimpleType(BigDecimal.class))
                            .isTrue());
        });
    }

    @Test
    void customConversionsShouldBeUsed() {
        this.contextRunner
                .withUserConfiguration(CustomOpenSearchCustomConversions.class)
                .run((context) -> {
                    assertThat(context)
                            .hasSingleBean(ElasticsearchCustomConversions.class)
                            .hasBean("testCustomConversions");
                    assertThat(context.getBean(ElasticsearchConverter.class)
                                    .getConversionService()
                                    .canConvert(OpenSearchRestTemplate.class, Boolean.class))
                            .isTrue();
                });
    }

    @Test
    void customRestTemplateShouldBeUsed() {
        this.contextRunner.withUserConfiguration(CustomRestTemplate.class).run((context) -> assertThat(context)
                .getBeanNames(OpenSearchRestTemplate.class)
                .hasSize(1)
                .contains("opensearchTemplate"));
    }

    @Test
    void shouldFilterInitialEntityScanWithDocumentAnnotation() {
        this.contextRunner.withUserConfiguration(EntityScanConfig.class).run((context) -> {
            SimpleElasticsearchMappingContext mappingContext = context.getBean(SimpleElasticsearchMappingContext.class);
            assertThat(mappingContext.hasPersistentEntityFor(Product.class)).isTrue();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomOpenSearchCustomConversions {

        @Bean
        ElasticsearchCustomConversions testCustomConversions() {
            return new ElasticsearchCustomConversions(Collections.singletonList(new MyConverter()));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRestTemplate {
        @Bean
        OpenSearchRestTemplate opensearchTemplate() {
            return mock(OpenSearchRestTemplate.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class EntityScanConfig {}

    static class MyConverter implements Converter<OpenSearchRestTemplate, Boolean> {

        @Override
        public Boolean convert(OpenSearchRestTemplate source) {
            return null;
        }
    }
}
