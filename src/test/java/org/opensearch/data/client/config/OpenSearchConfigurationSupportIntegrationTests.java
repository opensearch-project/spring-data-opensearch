/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration;
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.junit.jupiter.Tags;

/**
 * Integration tests for {@link OpenSearchConfigurationSupport}.
 */
@Tag(Tags.INTEGRATION_TEST)
public class OpenSearchConfigurationSupportIntegrationTests {

    @Test // DATAES-504
    public void usesConfigClassPackageAsBaseMappingPackage() throws ClassNotFoundException {

        var configuration = new StubConfig();
        assertThat(configuration.getMappingBasePackages())
                .contains(StubConfig.class.getPackage().getName());
        assertThat(configuration.getInitialEntitySet()).contains(Entity.class);
    }

    @Test // DATAES-504
    public void doesNotScanOnEmptyBasePackage() throws ClassNotFoundException {

        var configuration = new StubConfig() {
            @Override
            public Collection<String> getMappingBasePackages() {
                return Collections.emptySet();
            }
        };

        assertThat(configuration.getInitialEntitySet()).isEmpty();
    }

    @Test // DATAES-504
    public void containsMappingContext() {

        AbstractApplicationContext context = new AnnotationConfigApplicationContext(StubConfig.class);
        assertThat(context.getBean(SimpleElasticsearchMappingContext.class)).isNotNull();
    }

    @Test // DATAES-504
    public void containsElasticsearchConverter() {

        AbstractApplicationContext context = new AnnotationConfigApplicationContext(StubConfig.class);
        assertThat(context.getBean(ElasticsearchConverter.class)).isNotNull();
    }

    @Test // DATAES-504
    public void restConfigContainsOpenSearchTemplate() {

        AbstractApplicationContext context = new AnnotationConfigApplicationContext(RestConfig.class);
        assertThat(context.getBean(OpenSearchRestTemplate.class)).isNotNull();
    }

    @Test // DATAES-563
    public void restConfigContainsElasticsearchOperationsByNameAndAlias() {

        AbstractApplicationContext context = new AnnotationConfigApplicationContext(RestConfig.class);

        assertThat(context.getBean("elasticsearchOperations")).isNotNull();
        assertThat(context.getBean("elasticsearchTemplate")).isNotNull();
    }

    @Configuration
    static class StubConfig extends ElasticsearchConfigurationSupport {
        @Override
        public Set<Class<?>> getInitialEntitySet() {
            return super.getInitialEntitySet();
        }

        @Override
        public Collection<String> getMappingBasePackages() {
            return super.getMappingBasePackages();
        }
    }

    @Configuration
    static class RestConfig extends AbstractOpenSearchConfiguration {

        @Override
        public RestHighLevelClient opensearchClient() {
            return mock(RestHighLevelClient.class);
        }
    }

    @Configuration
    static class EntityMapperConfig extends ElasticsearchConfigurationSupport {}

    @Document(indexName = "config-support-tests")
    static class Entity {}
}
