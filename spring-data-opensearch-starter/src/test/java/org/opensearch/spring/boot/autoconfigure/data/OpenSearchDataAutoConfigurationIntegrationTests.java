/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;
import org.opensearch.spring.boot.autoconfigure.AbstractOpenSearchIntegrationTest;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestHighLevelClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.data.entity.Product;
import org.opensearch.spring.boot.autoconfigure.data.entity.ProductOpenSearchRepository;
import org.opensearch.spring.boot.autoconfigure.data.repository.ProductRepository;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Tests for {@link OpenSearchDataAutoConfiguration}.
 */
@Testcontainers(disabledWithoutDocker = true)
class OpenSearchDataAutoConfigurationIntegrationTests extends AbstractOpenSearchIntegrationTest {
    @Container
    static final OpensearchContainer opensearch = new OpensearchContainer(getDockerImageName())
            .withStartupAttempts(5)
            .withStartupTimeout(Duration.ofMinutes(10));

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    OpenSearchRestClientAutoConfiguration.class,
                    OpenSearchRestHighLevelClientAutoConfiguration.class,
                    ElasticsearchRepositoriesAutoConfiguration.class,
                    OpenSearchDataAutoConfiguration.class))
            .withPropertyValues("opensearch.uris=" + opensearch.getHttpHostAddress());

    @Test
    void defaultRepositoryConfiguration() {
        this.contextRunner.withUserConfiguration(TestConfiguration.class).run((context) -> assertThat(context)
                .hasSingleBean(ProductOpenSearchRepository.class)
                .hasSingleBean(OpenSearchRestTemplate.class));
    }

    @Test
    void noRepositoryConfiguration() {
        this.contextRunner
                .withInitializer(context -> AutoConfigurationPackages.register(
                        (BeanDefinitionRegistry) context.getBeanFactory(),
                        "org.opensearch.spring.boot.autoconfigure.data.empty"))
                .run((context) -> assertThat(context).hasSingleBean(OpenSearchRestTemplate.class));
    }

    @Test
    void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
        this.contextRunner.withUserConfiguration(CustomizedConfiguration.class).run((context) -> assertThat(context)
                .hasSingleBean(ProductRepository.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableElasticsearchRepositories(basePackageClasses = Product.class)
    static class TestConfiguration {}

    @Configuration(proxyBeanMethods = false)
    @EnableElasticsearchRepositories(basePackageClasses = ProductRepository.class)
    static class CustomizedConfiguration {}
}
