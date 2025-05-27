/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.data;

import java.util.Collections;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.data.client.osc.OpenSearchTemplate;
import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.opensearch.data.client.osc.ReactiveOpenSearchTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

/**
 * Configuration classes for Spring Data for Opensearch
 * <p>
 * Those should be {@code @Import} in a regular auto-configuration class to guarantee
 * their order of execution.
 *
 * Adaptation of the {@link org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataConfiguration} to
 * the needs of OpenSearch.
 */
abstract class OpenSearchDataConfiguration {

    @Configuration(proxyBeanMethods = false)
    static class BaseConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ElasticsearchCustomConversions elasticsearchCustomConversions() {
            return new ElasticsearchCustomConversions(Collections.emptyList());
        }

        @Bean
        @ConditionalOnMissingBean
        SimpleElasticsearchMappingContext mappingContext(
                ApplicationContext applicationContext, ElasticsearchCustomConversions elasticsearchCustomConversions)
                throws ClassNotFoundException {
            SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
            mappingContext.setInitialEntitySet(new EntityScanner(applicationContext).scan(Document.class));
            mappingContext.setSimpleTypeHolder(elasticsearchCustomConversions.getSimpleTypeHolder());
            return mappingContext;
        }

        @Bean
        @ConditionalOnMissingBean
        ElasticsearchConverter elasticsearchConverter(
                SimpleElasticsearchMappingContext mappingContext,
                ElasticsearchCustomConversions elasticsearchCustomConversions) {
            MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
            converter.setConversions(elasticsearchCustomConversions);
            return converter;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(OpenSearchClient.class)
    static class JavaClientConfiguration {

        @Bean
        @ConditionalOnMissingBean(value = ElasticsearchOperations.class, name = { "elasticsearchTemplate", "opensearchTemplate" })
        @ConditionalOnBean(OpenSearchClient.class)
        OpenSearchTemplate elasticsearchTemplate(OpenSearchClient client, ElasticsearchConverter converter) {
            return new OpenSearchTemplate(client, converter);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(OpenSearchClient.class)
    static class ReactiveRestClientConfiguration {

        @Bean
        @ConditionalOnMissingBean(value = ReactiveElasticsearchOperations.class, name = { "reactiveElasticsearchTemplate", "reactiveOpensearchTemplate" })
        @ConditionalOnBean(ReactiveOpenSearchClient.class)
        ReactiveOpenSearchTemplate reactiveElasticsearchTemplate(ReactiveOpenSearchClient client,
                ElasticsearchConverter converter) {
            return new ReactiveOpenSearchTemplate(client, converter);
        }

    }

}
